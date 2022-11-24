package org.example;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.util.StringUtils;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.stream.Collectors;

@Getter
@Slf4j
@ToString
public class LdapUser {
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String loginId;
    protected String department;
    protected String properties;
    private String groupId;

    public LdapUser(DirContextAdapter dirContextAdapter, final LdapProperty ldapProperty) {
        final var attributes = dirContextAdapter.getAttributes();

        this.loginId = extractAttribute(attributes, ldapProperty.getUserIdAttribute());
        this.email = extractAttribute(attributes, ldapProperty.getUserEmailAttributes().toArray(new String[0]));
        this.firstName = extractAttribute(attributes, "displayname", "cn");
        this.properties = getOrganisationalUnits(dirContextAdapter);

        if (ldapProperty.supportGroup()) {
            this.groupId = extractAttribute(attributes, ldapProperty.getUserMembershipAttribute());
        }
    }

    private String extractAttribute(Attributes attributes, String... keys) {
        try {
            for (String key : keys) {
                if (attributes.get(key) != null) {
                    return attributes.get(key).get().toString();
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private String getOrganisationalUnits(DirContextAdapter dirContextAdapter) {
        try {
            final var base = new LdapName(dirContextAdapter.getNameInNamespace());

            return base.getRdns().stream()
                    .filter(rdn -> StringUtils.startsWithIgnoreCase(rdn.getType(),"ou"))
                    .map(Rdn::toString)
                    .collect(Collectors.joining(","));
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }
    }
}
