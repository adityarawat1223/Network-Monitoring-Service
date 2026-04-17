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

@RestController
public class SshController {
    private final String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_monitoring";


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

    private double CpuCalc(SSHClient ssh) throws TransportException, ConnectionException, InterruptedException {
        ArrayList<Integer> arr1, arr2;

        try (Session session = ssh.startSession();
             Session.Command cmd = session.exec("grep '^cpu ' /proc/stat")) {
            arr1 = Reader(cmd.getInputStream());
        }

        Thread.sleep(2000);

        try (Session session = ssh.startSession();
             Session.Command cmd2 = session.exec("grep '^cpu ' /proc/stat")) {
            arr2 = Reader(cmd2.getInputStream());
        }

        long tot1 = 0, tot2 = 0;
        for (var ele : arr1) tot1 += ele;
        for (var ele : arr2) tot2 += ele;

        long totalDelta = tot2 - tot1;
        long idleDelta = (long)(arr2.get(3) + arr2.get(4)) - (arr1.get(3) + arr1.get(4));

        return totalDelta == 0 ? 0.0 : ((double)(totalDelta - idleDelta) / totalDelta) * 100.0;
    }
    private double MemCalc(SSHClient ssh) throws TransportException, ConnectionException {
        double Total, Free;
        try (Session session = ssh.startSession();
             Session.Command cmd = session.exec("grep '^MemTotal' /proc/meminfo")) {
            Scanner sc = new Scanner(cmd.getInputStream());
            sc.next();
            Total = sc.nextInt();
        }

        try (Session session = ssh.startSession();
             Session.Command cmd = session.exec("grep '^MemFree' /proc/meminfo")) {
            Scanner sc = new Scanner(cmd.getInputStream());
            sc.next();
            Free = sc.nextInt();
        }
        return Total - Free;
    }
    private double Uptime(SSHClient ssh) throws TransportException, ConnectionException {
        try (Session session = ssh.startSession();
             Session.Command cmd = session.exec("cat /proc/uptime")) {
            Scanner sc = new Scanner(cmd.getInputStream());
            return sc.nextDouble();
        }
    }


    @GetMapping("/sysinfo")
    public Response execute() throws IOException, InterruptedException {

        try (SSHClient ssh = new SSHClient()) {

            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            String remoteHost = "172.22.29.205";
            ssh.connect(remoteHost);
            String username = "burstingfire355";
            ssh.authPublickey(username, privateKeyPath);

            Response response = new Response();
            response.CpuUsage = CpuCalc(ssh);
            response.MemoryUsage = MemCalc(ssh);
            response.UpTime = "Total Uptime is " + Uptime(ssh);

            return response;
        }

    }
}
