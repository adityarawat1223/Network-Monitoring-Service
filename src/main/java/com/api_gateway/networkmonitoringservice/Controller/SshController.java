package com.api_gateway.networkmonitoringservice.Controller;
import com.api_gateway.networkmonitoringservice.dto.Response;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import net.schmizz.sshj.SSHClient;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;
import org.springframework.data.redis.core.StringRedisTemplate;


@RestController
public class SshController {
    private final String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_monitoring";
    private final StringRedisTemplate redis;

    public SshController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private ArrayList<Integer> Reader(InputStream val){
        try(Scanner sc = new Scanner(val)) {
            sc.next();
            ArrayList<Integer> arr = new ArrayList<>();
            while (arr.size() < 8 && sc.hasNextInt()) {
                arr.add(sc.nextInt());
            }
            return arr;
        }
    }

    private double CpuCalc(SSHClient ssh) throws TransportException, ConnectionException {
        ArrayList<Integer> arr;

        String totstring = redis.opsForValue().get("total");
        String idlestring =  redis.opsForValue().get("idle");
        try (Session session = ssh.startSession();
             Session.Command cmd = session.exec("grep '^cpu ' /proc/stat")) {
            arr = Reader(cmd.getInputStream());
        }


        long tot1 = 0, tot2 = 0 , idle1 = (arr.get(3) + arr.get(4)) , idle2 = 0;
        for (var ele : arr) tot1 += ele;

        if(totstring != null){
          tot2 =  Long.parseLong(totstring);
        }
        if(idlestring != null){
            idle2 = Long.parseLong(idlestring);
        }

        redis.opsForValue().set("total",Long.toString(tot1));
        redis.opsForValue().set("idle",Long.toString(idle1));

        long totalDelta = tot1-tot2;
        long idleDelta = (idle1) - (idle2);
        if (totalDelta < 0) return 0.0;
        return totalDelta == 0 ? 0.0 : ((double)(totalDelta - idleDelta) / totalDelta) * 100.0;
    }
    private double MemCalc(SSHClient ssh) throws TransportException, ConnectionException {
        double Total, Free;
        try (Session session = ssh.startSession();
             Session.Command cmd = session.exec("grep '^MemTotal' /proc/meminfo")) {
            try(Scanner sc = new Scanner(cmd.getInputStream())) {
                sc.next();
                Total = sc.nextInt();
            }
        }

        try (Session session = ssh.startSession();
             Session.Command cmd = session.exec("grep '^MemAvailable' /proc/meminfo")) {
            try(Scanner sc = new Scanner(cmd.getInputStream())) {
                sc.next();
                Free = sc.nextInt();
            }
        }
        return Total - Free;
    }
    private double Uptime(SSHClient ssh) throws TransportException, ConnectionException {
        try (Session session = ssh.startSession();
             Session.Command cmd = session.exec("cat /proc/uptime")) {
            try(Scanner sc = new Scanner(cmd.getInputStream())) {
                return sc.nextDouble();
            }
        }
    }


    @GetMapping("/sysinfo")
    public Response execute() throws IOException {

        try (SSHClient ssh = new SSHClient()) {

            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            String remoteHost = "172.22.29.205";
            ssh.connect(remoteHost);
            String username = "burstingfire355";
            ssh.authPublickey(username, privateKeyPath);

            Response response = new Response();
            response.CpuUsage = CpuCalc(ssh);
            response.MemoryUsage = MemCalc(ssh);
            double uptime = Uptime(ssh);
            response.UpTime = "Total Uptime is " + (uptime) / 3600 + " hr and " + ((uptime % 3600) / 60) + "mins";
            return response;

        }

    }
}
