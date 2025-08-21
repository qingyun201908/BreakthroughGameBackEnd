package BreakthroughGame.backEnd.UserModule.UserRestful;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@EnableJpaRepositories(basePackages = {
        "BreakthroughGame.backEnd.UserModule.UserInfo.repository","BreakthroughGame.backEnd.WarcraftDungeonsInfo.repository"
})
@EntityScan(basePackages = {
        "BreakthroughGame.backEnd.UserModule.UserInfo.entity","BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity"
})
@SpringBootApplication(scanBasePackages = {"BreakthroughGame.backEnd"})
public class UserAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserAuthApplication.class, args);
    }
}