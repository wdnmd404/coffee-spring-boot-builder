package site.zido.coffee.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;
import site.zido.coffee.security.authentication.IdUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

/**
 * @author zido
 */
public class JwtSecurityContextRepository implements SecurityContextRepository {
    public static final String DEFAULT_AUTH_HEADER_NAME = "Authorization";
    private static Logger LOGGER = LoggerFactory.getLogger(JwtSecurityContextRepository.class);
    private String authHeaderName = DEFAULT_AUTH_HEADER_NAME;
    private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    private JwtTokenProvider tokenProvider;

    public JwtSecurityContextRepository(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        HttpServletRequest request = requestResponseHolder.getRequest();
        String token = request.getHeader(authHeaderName);
        Object authentication = tokenProvider.getAuthenticationFromJwt(token);
        if (authentication == null) {
            authentication = generateNewContext();
        }
        if (!(authentication instanceof SecurityContext)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("jwt did not contain a SecurityContext but contained: '"
                        + authentication
                        + "'; are you improperly modifying the HttpSession directly "
                        + "(you should always use SecurityContextHolder) or using the Authentication attribute "
                        + "reserved for this class?");
            }

            authentication = generateNewContext();
        }

        LOGGER.debug("Obtained a valid SecurityContext from " + authHeaderName
                + " in request header"
                + ": '" + authentication + "'");
        return (SecurityContext) authentication;
    }

    protected SecurityContext generateNewContext() {
        return SecurityContextHolder.createEmptyContext();
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = context.getAuthentication();
        if (authentication != null && !trustResolver.isAnonymous(authentication)) {
            Object subject;
            if (authentication instanceof IdUser) {
                IdUser<? extends Serializable> userPrincipal = (IdUser<? extends Serializable>) authentication.getPrincipal();
                subject = userPrincipal.getId();
            } else {
                throw new IllegalStateException("authentication cannot find id");
            }
            String token = tokenProvider.generateToken(subject);
            response.setHeader(authHeaderName, token);
            LOGGER.debug("SecurityContext '" + context
                    + "' stored to response.header: " + authHeaderName);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return StringUtils.hasLength(request.getHeader(authHeaderName));
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }

    public void setTokenProvider(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
        this.trustResolver = trustResolver;
    }
}
