package rewards;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class JdbcBootController {
    
    @GetMapping("/")
    public String hello() {
        return "Hello!";
    }
}
