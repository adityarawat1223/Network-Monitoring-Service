package com.api_gateway.networkmonitoringservice.Controller;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import net.schmizz.sshj.SSHClient;
import java.io.IOException;

@RestController
public class SshController {
    private final String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_monitoring";

    @GetMapping("/sysinfo")
    public String execute () throws IOException {

      try (SSHClient ssh = new SSHClient()){

          ssh.addHostKeyVerifier(new PromiscuousVerifier());
          String remoteHost = "172.22.29.205";
          ssh.connect(remoteHost);
          String username = "burstingfire355";
          ssh.authPublickey(username,privateKeyPath);

          try (Session session = ssh.startSession()){
              Session.Command cmd = session.exec("top -b -n 1 | grep \"Cpu(s)\"");

              return IOUtils.readFully(cmd.getInputStream()).toString();

          }

      }

    }
}
