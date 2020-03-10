package site.zido.demo.api;

import site.zido.demo.entity.Admin;
import site.zido.demo.entity.User;
import site.zido.demo.pojo.params.UserParams;
import site.zido.demo.service.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('a') || authentication.principal instanceof T(site.zido.demo.entity.User)")
    @GetMapping("/me")
    public User user(@AuthenticationPrincipal(expression = "user") User user) {
        return user;
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addUser(UserParams params, @AuthenticationPrincipal Admin admin) {
        userService.addUser(params, admin);
    }

}
