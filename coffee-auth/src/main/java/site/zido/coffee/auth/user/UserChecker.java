package site.zido.coffee.auth.user;

/**
 * @author zido
 */
public interface UserChecker {
    void check(UserDetails toCheck);
}