package site.zido.demo.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.zido.coffee.extra.limiter.Limiter;

/**
 * 一些简单接口使用
 *
 * @author zido
 */
@RestController
public class IndexController {

    @PreAuthorize("hasAuthority('ROLE_user')")
    @RequestMapping("/hello")
    public String index(@AuthenticationPrincipal UserDetails user) {
        return "hello world : " + user.getUsername();
    }

    @RequestMapping("/limit")
    @Limiter(key = "content")
    public String limit() {
        return "limit content";
    }

    @PreAuthorize("hasAuthority('ROLE_admin')")
    @RequestMapping("/admin")
    public String admin(@AuthenticationPrincipal UserDetails user) {
        return "hello world : " + user.getUsername();
    }
}
