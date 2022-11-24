package org.example;

import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.core.support.SingleContextSource;
import org.springframework.ldap.support.LdapUtils;

import javax.naming.directory.SearchControls;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LdapService {
    private final LdapTemplate ldapTemplate;

    private final LdapProperty ldapProperty;

    private final LdapContextSource ldapContextSource;

    public LdapService(LdapProperty ldapProperty, Long connectionTimeout) {
        trustSelfSignedSSL();

        this.ldapProperty = ldapProperty;

        this.ldapContextSource = new LdapContextSource();
        ldapContextSource.setBase(ldapProperty.getUserBaseDn());
        ldapContextSource.setUrl(ldapProperty.getUrl());
        ldapContextSource.setUserDn(ldapProperty.getBindDn());
        ldapContextSource.setAnonymousReadOnly(ldapProperty.isAnonymous());
        ldapContextSource.setBaseEnvironmentProperties(Map.of("com.sun.jndi.ldap.connect.timeout", connectionTimeout.toString()));

        if (!ldapProperty.getBindPassword().isEmpty()) {
            ldapContextSource.setPassword(ldapProperty.getBindPassword());
        }

        ldapContextSource.afterPropertiesSet();

        this.ldapTemplate = new LdapTemplate(ldapContextSource);
    }

    public List<LdapUser> findAll() {
        final var filter = getFilter("*");

        final var searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        final var processor = new PagedResultsDirContextProcessor(100);

        return SingleContextSource.doWithSingleContext(ldapContextSource, operations -> {
            final var result = new ArrayList<LdapUser>();

            do {
                final var users = operations.search(
                        LdapUtils.emptyLdapName(),
                        filter,
                        searchControls,
                        (ContextMapper<LdapUser>) ctx -> new LdapUser((DirContextAdapter) ctx, ldapProperty),
                        processor);

                result.addAll(users);
            } while (processor.hasMore());

            return result;
        });
    }

    public boolean authenticate(final String parameter, final String password) {
        final var filter = getFilter(parameter);

        boolean result = false;

        try {
            result = ldapTemplate.authenticate(LdapUtils.emptyLdapName(), filter, password, (e) -> {
                if (e instanceof AuthenticationException) {
                    throw new RuntimeException(e.getMessage());
                }
            });
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains("code 49")) {
                throw new RuntimeException(e.getMessage());
            }

            throw new RuntimeException(parseErrorMessage(e.getMessage()), e);
        }

        if (!result) {
            throw new RuntimeException("Not Found User in LDAP");
        }

        return true;
    }

    private String parseErrorMessage(final String originMessage) {
        final var startIndex = originMessage.indexOf("[");
        final var endIndex = originMessage.indexOf("]");

        if (startIndex != -1 && endIndex != -1) {
            return originMessage.substring(startIndex, endIndex + 1);
        }

        return originMessage;
    }

    private String getFilter(final String parameter) {
        var filter = MessageFormat.format("{0}={1}", ldapProperty.getUserIdAttribute(), parameter);

        if (!(ldapProperty.getUserFilter().isEmpty())) {
            filter = MessageFormat.format("(&({0})({1}))", ldapProperty.getUserFilter(), filter);
        }

        return filter;
    }

    public static void trustSelfSignedSSL() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {

                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLContext.setDefault(ctx);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
