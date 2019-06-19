package site.zido.coffee.auth.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;
import site.zido.coffee.auth.context.UserHolder;
import site.zido.coffee.auth.entity.IUser;
import site.zido.coffee.auth.exceptions.AuthenticationException;
import site.zido.coffee.auth.exceptions.InternalAuthenticationException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class AuthFilterBean extends GenericFilterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilterBean.class);
    private static final String REQUEST_METHOD = "POST";
    private Map<String, AuthHandler<? extends IUser, ? extends java.io.Serializable>> handlerMap;
    private UrlPathHelper urlPathHelper;
    private LoginFailureHandler failureHandler;
    private LoginSuccessHandler successHandler;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String currentUrl = getRequestPath(request);
        AuthHandler<? extends IUser, ? extends Serializable> authHandler = handlerMap.get(currentUrl);
        if (authHandler != null && REQUEST_METHOD.equals(request.getMethod())) {
            LOGGER.debug("请求认证开始");
            IUser authResult;
            try {
                authResult = authHandler.attempAuthentication(request, response);
                if (authResult == null) {
                    return;
                }
            } catch (InternalAuthenticationException failed) {
                logger.error(
                        "An internal error occurred while trying to authenticate the user.",
                        failed);
                unsuccessfulAuthentication(request, response, failed);
                return;
            } catch (AuthenticationException failed) {
                unsuccessfulAuthentication(request, response, failed);
                return;
            }
            successfulAuthentication(request, response, chain, authResult);
        } else {
            chain.doFilter(request, response);
        }
    }

    private void successfulAuthentication(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          IUser authResult) throws IOException, ServletException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("认证成功，更新userHolder:" + authResult);
        }
        UserHolder.set(authResult);
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }

    private void unsuccessfulAuthentication(HttpServletRequest request,
                                            ServletResponse response,
                                            AuthenticationException failed) {
        UserHolder.clearContext();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("请求认证失败:" + failed.toString(), failed);
        }

    }

    private String getRequestPath(HttpServletRequest request) {
        if (this.urlPathHelper != null) {
            return this.urlPathHelper.getPathWithinApplication(request);
        }
        String url = request.getServletPath();

        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            url = StringUtils.hasLength(url) ? url + pathInfo : pathInfo;
        }

        return url;
    }

    public void setAuthenticationFailureHandler(
            LoginFailureHandler failureHandler) {
        Assert.notNull(failureHandler, "failureHandler cannot be null");
        this.failureHandler = failureHandler;
    }

    public void setAuthenticationSuccessHandler(
            LoginSuccessHandler successHandler) {
        Assert.notNull(successHandler, "successHandler cannot be null");
        this.successHandler = successHandler;
    }

    public void setHandlerMap(Map<String, AuthHandler<? extends IUser, ? extends Serializable>> handlerMap) {
        this.handlerMap = handlerMap;
    }

    public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
        this.urlPathHelper = urlPathHelper;
    }
}
