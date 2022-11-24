package org.example;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        final var url = "ldap://127.0.0.1:389";
        final var userBaseDn = "dc=rogan,dc=test,dc=io";
        final var userSearchFilter = "objectclass=inetOrgPerson";
//        final var anonymous = false;
        final var anonymous = true;

        final var bindDn = "cn=admin,dc=rogan,dc=test,dc=io";
        final var bindPwd = "1234";
        final var userId = "uid";
        final var userEmail = "email";
        final var newUserInactive = false;
        final var syncGroup = false;

        final var connectionTimeout = 50000L;

        final var ldapProperty = new LdapProperty(url, bindDn, bindPwd, anonymous, userBaseDn, userSearchFilter, userId, List.of(userEmail), "", "", "", "", newUserInactive, syncGroup);
        final var ldapService = new LdapService(ldapProperty, connectionTimeout);

        final var ldapUsers = ldapService.findAll();

        System.out.println("-----------------------------------------");
        System.out.println(ldapUsers);
        System.out.println("-----------------------------------------");

        if (anonymous) {
            System.out.println("anonymous\n");

            if (ldapService.authenticate("rogan", "1234")) {
                System.out.println("success");

            } else {
                System.out.println("fail");
            }

        } else {
            System.out.println("non anonymous\n");

            if (ldapService.authenticate("rogan", "1234")) {
                System.out.println("success");

            } else {
                System.out.println("fail");
            }
        }

        System.out.println("\n-----------------------------------------");
    }
}