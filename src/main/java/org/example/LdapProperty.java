package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LdapProperty {
    private String url;
    private String bindDn;
    private String bindPassword;
    private boolean anonymous;

    private String userBaseDn;
    private String userFilter;
    private String userIdAttribute;
    private List<String> userEmailAttributes;

    private String groupBaseDn;
    private String groupFilter;
    private String groupIdAttribute;

    private String userMembershipAttribute;

    private boolean newUserInactive;
    private boolean syncGroup;

    public boolean supportGroup() {
        return syncGroup;
    }

}
