package testperfix.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class WebApp {
    public static void main(String[] args) {
        String includesProperty = System.getProperty("perfix.includes");
        if (includesProperty == null || !includesProperty.equals("testperfix")) {
            System.out.println("Start me with -javaagent:target/agent-0.1-SNAPSHOT.jar -Dperfix.includes=testperfix");

            System.out.println("Exiting now");
            System.exit(0);
        }

        SpringApplication.run(WebApp.class, args);
        System.out.println("Perfix Test Web Application is running");
    }


    @RequestMapping("/greetings")
    public String index() {
        return greetings();
    }

    private String greetings() {
        return "Greetings from Spring Boot!";
    }

}
