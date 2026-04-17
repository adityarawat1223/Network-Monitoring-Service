package com.api_gateway.networkmonitoringservice.Controller;
import com.api_gateway.networkmonitoringservice.dto.Response;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import net.schmizz.sshj.SSHClient;
import java.io.*;
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
    static InputStream toStream(String line) {
        return new ByteArrayInputStream(line.getBytes());
    }

    private ArrayList<Long> Reader(InputStream val){
        try(Scanner sc = new Scanner(val)) {
            sc.next();
            ArrayList<Long> arr = new ArrayList<>();
            while (arr.size() < 8 && sc.hasNextLong()) {
                arr.add(sc.nextLong());
            }
            return arr;
        }
    }

    private double CpuCalc(InputStream stream)  {
        ArrayList<Long> arr;

        String totstring = redis.opsForValue().get("total");
        String idlestring =  redis.opsForValue().get("idle");
        arr = Reader(stream);
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
    private double MemCalc(InputStream stream1 , InputStream stream2) {
        double Total, Free;
            try(Scanner sc = new Scanner(stream1)) {
                sc.next();
                Total = sc.nextInt();
            }
            try(Scanner sc = new Scanner(stream2)) {
                sc.next();
                Free = sc.nextInt();
            }

        return ((Total - Free) / Total) * 100;
    }
    private double Uptime(InputStream stream) {

            try(Scanner sc = new Scanner(stream)) {
                return sc.nextDouble();
            }

    }

    private ArrayList<InputStream> InputGiver  (SSHClient ssh) throws  IOException{
        ArrayList<InputStream>arr = new ArrayList<>();
        try (Session session = ssh.startSession()){
            String command = "grep '^cpu ' /proc/stat;grep '^MemTotal' /proc/meminfo; grep '^MemAvailable' /proc/meminfo;cat /proc/uptime";
            Session.Command cmd = session.exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
            arr.add(toStream(reader.readLine()));
            arr.add(toStream(reader.readLine()));
            arr.add(toStream(reader.readLine()));
            arr.add(toStream(reader.readLine()));
        }

        return arr;
    }
    @Scheduled(fixedDelay = 30000)
    public void SysPoller ()  throws IOException {

        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect("172.22.29.205");
            ssh.authPublickey("burstingfire355", privateKeyPath);
            ArrayList<InputStream> arr = InputGiver(ssh);
            redis.opsForValue().set("CpuUsage", Double.toString(CpuCalc(arr.get(0))));
            redis.opsForValue().set("MemUsage", Double.toString(MemCalc(arr.get(1) , arr.get(2))));
            redis.opsForValue().set("UpTime", Double.toString(Uptime(arr.get(3))));
        }

    }
    @GetMapping("/sysinfo")
    public Response execute()  {

        Response response = new Response();
        String CpuUsage = redis.opsForValue().get("CpuUsage");
        String MemUsage = redis.opsForValue().get("MemUsage");
        String Uptime = redis.opsForValue().get("UpTime");
        double temp = (Uptime == null ? 0 : Double.parseDouble(Uptime));
        response.CpuUsage = (CpuUsage == null ? 0 : Math.round(Double.parseDouble(CpuUsage))) ;
        response.MemoryUsage = (MemUsage == null ? 0 : Math.round(Double.parseDouble(MemUsage))) ;
        response.UpTime = "System Uptime is " + Math.round(temp / 3600) + " Hr and " + Math.round((temp%3600)/60) + " Mins";
        return response;

    }
}
