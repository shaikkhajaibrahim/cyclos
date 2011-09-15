/*
 This file is part of Cyclos.

 Cyclos is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Cyclos is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Cyclos; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 */
package mp.platform.cyclone.webservices.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jws.WebService;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.PendingMember;
import nl.strohalm.cyclos.entities.members.RegisteredMember;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import mp.platform.cyclone.webservices.WebServiceContext;
import mp.platform.cyclone.webservices.model.GroupVO;
import mp.platform.cyclone.webservices.model.MemberVO;
import mp.platform.cyclone.webservices.model.RegistrationFieldValueVO;
import mp.platform.cyclone.webservices.utils.server.ChannelHelper;
import mp.platform.cyclone.webservices.utils.server.GroupHelper;
import mp.platform.cyclone.webservices.utils.server.MemberHelper;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang.StringUtils;

/**
 * Web service implementation
 * @author luis
 */
@WebService(name = "members", serviceName = "members")
public class MemberWebServiceImpl implements MemberWebService {

    private static final Relationship[] FETCH = { Element.Relationships.USER, Element.Relationships.GROUP, Member.Relationships.IMAGES, Member.Relationships.CUSTOM_VALUES };
    private ElementService              elementService;
    private CustomFieldService          customFieldService;
    private MemberHelper                memberHelper;
    private GroupHelper                 groupHelper;
    private ChannelHelper               channelHelper;

    @SuppressWarnings("unchecked")
    public MemberResultPage fullTextSearch(final FullTextMemberSearchParameters params) {
        if (params == null) {
            return null;
        }

        final List<Member> members = (List<Member>) elementService.fullTextSearch(memberHelper.toFullTextQuery(params));
        return memberHelper.toResultPage(params, members);
    }

    @SuppressWarnings("unchecked")
    public List<GroupVO> listManagedGroups() {
        final ServiceClient client = WebServiceContext.getClient();
        final List<GroupVO> groups = new ArrayList<GroupVO>();
        for (final MemberGroup group : client.getManageGroups()) {
            groups.add(groupHelper.toVO(group));
        }
        Collections.sort(groups, new BeanComparator("name"));
        return groups;
    }

    @SuppressWarnings("unchecked")
    public MemberVO load(final long id) {
        try {
            final Element element = elementService.load(id, FETCH);
            if (element instanceof Member) {
                final List<MemberCustomField> allFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
                final List<MemberCustomField> fields = CustomFieldHelper.onlyForGroup(allFields, (MemberGroup) element.getGroup());
                return memberHelper.toVO((Member) element, fields, true);
            }
        } catch (final PermissionDeniedException e) {
        } catch (final EntityNotFoundException e) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public MemberVO loadByUsername(final String username) {
        try {
            final User user = elementService.loadUser(username, RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP), RelationshipHelper.nested(User.Relationships.ELEMENT, Member.Relationships.CUSTOM_VALUES), RelationshipHelper.nested(User.Relationships.ELEMENT, Member.Relationships.IMAGES));
            if (user instanceof MemberUser) {
                final Member member = ((MemberUser) user).getMember();
                final List<MemberCustomField> allFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
                final List<MemberCustomField> fields = CustomFieldHelper.onlyForGroup(allFields, member.getMemberGroup());
                return memberHelper.toVO(member, fields, true);
            }
        } catch (final PermissionDeniedException e) {
        } catch (final EntityNotFoundException e) {
        }
        return null;
    }

    public MemberRegistrationResult registerMember(final RegisterMemberParameters params) {
        if (params == null) {
            throw new IllegalArgumentException();
        }

        // When the generic 'credentials' is passed in, we need to either set the login password or pin
        final String credentials = params.getCredentials();
        if (StringUtils.isNotEmpty(credentials)) {
            params.setLoginPassword(null);
            params.setPin(null);

            final Channel channel = WebServiceContext.getChannel();
            if (channel != null) {
                switch (channel.getCredentials()) {
                    case DEFAULT:
                    case LOGIN_PASSWORD:
                        params.setLoginPassword(credentials);
                        break;
                    case PIN:
                        params.setPin(credentials);
                        break;
                }
            }
        }

        final Member member = memberHelper.toMember(params);
        member.getUser().setPassword(params.getLoginPassword());
        if (StringUtils.isNotEmpty(params.getPin())) {
            member.getMemberUser().setPin(params.getPin());
        }

        // Register the member
        final RegisteredMember registered = elementService.registerMemberByWebService(WebServiceContext.getClient(), member, WebServiceContext.getRequest().getRemoteAddr());
        final MemberRegistrationResult result = new MemberRegistrationResult();
        if (registered instanceof PendingMember) {
            result.setAwaitingEmailValidation(true);
        } else {
            result.setId(registered.getId());
            result.setUsername(registered.getUsername());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public MemberResultPage search(final MemberSearchParameters params) {
        if (params == null) {
            return null;
        }

        final List<Member> members = (List<Member>) elementService.search(memberHelper.toQuery(params));
        return memberHelper.toResultPage(params, members);
    }

    public void setChannelHelper(final ChannelHelper channelHelper) {
        this.channelHelper = channelHelper;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    public void setGroupHelper(final GroupHelper groupHelper) {
        this.groupHelper = groupHelper;
    }

    public void setMemberHelper(final MemberHelper memberHelper) {
        this.memberHelper = memberHelper;
    }

    @SuppressWarnings("unchecked")
    public void updateMember(final UpdateMemberParameters params) {
        final Long id = params == null || params.getId() == null || params.getId().intValue() <= 0 ? null : params.getId();
        final String principal = params == null || StringUtils.isEmpty(params.getPrincipal()) ? null : params.getPrincipal();
        Member member;
        if (id != null) {
            // Load by id, if passed
            try {
                member = (Member) elementService.load(id, FETCH);
            } catch (final Exception e) {
                throw new EntityNotFoundException();
            }
        } else if (principal != null) {
            // Load by principal, if passed
            final PrincipalType principalType = channelHelper.resolvePrincipalType(params.getPrincipalType());
            member = elementService.loadByPrincipal(principalType, params.getPrincipal(), FETCH);
        } else {
            // No identification was passed
            throw new IllegalArgumentException();
        }
        member = (Member) member.clone();
        // Update regular fields
        if (StringUtils.isNotEmpty(params.getName())) {
            member.setName(params.getName());
        }
        if (StringUtils.isNotEmpty(params.getEmail())) {
            member.setEmail(params.getEmail());
        }
        // Get the available custom fields
        List<MemberCustomField> customFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        customFields = CustomFieldHelper.onlyForGroup(customFields, member.getMemberGroup());
        final Collection<MemberCustomFieldValue> customValues = new ArrayList(member.getCustomValues());
        member.setCustomValues(customValues);
        final List<RegistrationFieldValueVO> fields = params.getFields();
        if (fields != null) {
            for (final RegistrationFieldValueVO vo : fields) {
                final String fieldName = vo.getField();
                final MemberCustomField field = CustomFieldHelper.findByInternalName(customFields, fieldName);
                if (field == null) {
                    throw new ValidationException();
                }
                MemberCustomFieldValue value = CustomFieldHelper.findByField(field, customValues);
                if (value == null) {
                    // No value for this field yet
                    value = new MemberCustomFieldValue();
                    value.setMember(member);
                    value.setField(field);
                    customValues.add(value);
                }
                value.setValue(vo.getValue());
                value.setHidden(vo.isHidden());
            }
        }
        elementService.changeMemberProfileByWebService(WebServiceContext.getClient(), member);
    }
}
