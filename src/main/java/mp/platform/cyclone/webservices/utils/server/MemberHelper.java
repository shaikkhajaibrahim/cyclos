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
package mp.platform.cyclone.webservices.utils.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.FullTextMemberQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.MemberQuery;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.query.Page;
import mp.platform.cyclone.webservices.PrincipalParameters;
import mp.platform.cyclone.webservices.WebServiceContext;
import mp.platform.cyclone.webservices.WebServiceFaultsEnum;
import mp.platform.cyclone.webservices.members.AbstractMemberSearchParameters;
import mp.platform.cyclone.webservices.members.FullTextMemberSearchParameters;
import mp.platform.cyclone.webservices.members.MemberResultPage;
import mp.platform.cyclone.webservices.members.MemberSearchParameters;
import mp.platform.cyclone.webservices.members.RegisterMemberParameters;
import mp.platform.cyclone.webservices.model.FieldValueVO;
import mp.platform.cyclone.webservices.model.MemberVO;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class for members
 * @author luis
 */
public class MemberHelper {

    private FetchService       fetchService;
    private QueryHelper        queryHelper;
    private ImageHelper        imageHelper;
    private FieldHelper        fieldHelper;
    private ElementService     elementService;
    private CustomFieldService customFieldService;
    private GroupService       groupService;
    private ChannelHelper      channelHelper;
    private AccessService      accessService;

    /**
     * Throws an invalid-channel fault if the current client's channel is not enabled for the given member
     */
    public void checkChannelEnabledForMember(final Member member) {
        if (!isChannelEnabledForMember(member)) {
            WebServiceFaultsEnum.INVALID_CHANNEL.throwFault();
        }
    }

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    public FieldHelper getFieldHelper() {
        return fieldHelper;
    }

    public ImageHelper getImageHelper() {
        return imageHelper;
    }

    /**
     * Returns whether the current client's channel is enabled for the given member
     */
    public boolean isChannelEnabledForMember(final Member member) {
        final Channel channel = WebServiceContext.getChannel();
        // If not restricted to a channel, no need to check
        if (channel == null) {
            return true;
        }
        // If the channel restricted member is the same member, no need to check
        final Member restrictedMember = WebServiceContext.getMember();
        if (restrictedMember != null && restrictedMember.equals(member)) {
            return true;
        }
        return accessService.isChannelEnabledForMember(channel.getInternalName(), member);
    }

    /**
     * Loads a member using the given principal, according to the current channel
     */
    public Member loadByPrincipal(PrincipalType principalType, final String principal) {
        if (StringUtils.isNotEmpty(principal)) {
            final Channel channel = WebServiceContext.getChannel();
            if (channel != null) {
                if (principalType == null) {
                    principalType = channel.getDefaultPrincipalType();
                } else if (!channel.getPrincipalTypes().contains(principalType)) {
                    return null;
                }
            }
            return elementService.loadByPrincipal(principalType, principal, Element.Relationships.USER);
        }
        return null;
    }

    /**
     * Loads a member using the given principal, according to the current channel
     */
    public Member loadByPrincipal(final String principalType, final String principal) {
        final PrincipalType type = channelHelper.resolvePrincipalType(principalType);
        return loadByPrincipal(type, principal);
    }

    /**
     * Checks the {@link ServiceClient#getMember()}. If restricted to a member, returns it, ignoring the received id. Otherwise, loads the member with
     * the given principal
     */
    public Member resolveMember(final PrincipalParameters params) {
        if (params == null) {
            return null;
        }
        return resolveMember(params.getPrincipalType(), params.getPrincipal());
    }

    /**
     * Checks the {@link ServiceClient#getMember()}. If restricted to a member, returns it, ignoring the received id. Otherwise, loads the member with
     * the given principal
     */
    public Member resolveMember(final PrincipalType principalType, final String principal) {
        final Member member = WebServiceContext.getMember();
        if (member == null) {
            if (StringUtils.isEmpty(principal)) {
                return null;
            }
            return loadByPrincipal(principalType, principal);
        } else {
            return member;
        }
    }

    /**
     * Checks the {@link ServiceClient#getMember()}. If restricted to a member, returns it, ignoring the received id. Otherwise, loads the member with
     * the given principal
     */
    public Member resolveMember(final String principalType, final String principal) {
        final PrincipalType type = channelHelper.resolvePrincipalType(principalType);
        return resolveMember(type, principal);
    }

    /**
     * Invokes resolveMember(String), returning it's user, or null
     */
    public MemberUser resolveUser(final PrincipalType principalType, final String principal) {
        final Member member = resolveMember(principalType, principal);
        return member == null ? null : member.getMemberUser();
    }

    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
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

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setFieldHelper(final FieldHelper fieldHelper) {
        this.fieldHelper = fieldHelper;
    }

    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    public void setImageHelper(final ImageHelper imageHelper) {
        this.imageHelper = imageHelper;
    }

    public void setQueryHelper(final QueryHelper queryHelper) {
        this.queryHelper = queryHelper;
    }

    public FullTextMemberQuery toFullTextQuery(final FullTextMemberSearchParameters params) {
        if (params == null) {
            return null;
        }
        final FullTextMemberQuery query = new FullTextMemberQuery();
        query.setEnabled(true);
        query.fetch(Element.Relationships.GROUP, Element.Relationships.USER);
        if (params.isShowCustomFields()) {
            query.fetch(Member.Relationships.CUSTOM_VALUES);
        }
        if (params.isShowImages()) {
            query.fetch(Member.Relationships.IMAGES);
        }
        query.setKeywords(params.getKeywords());
        final GroupFilter[] groupFilters = EntityHelper.references(GroupFilter.class, params.getGroupFilterIds());
        if (groupFilters == null || groupFilters.length > 0) {
            query.setGroupFilters(Arrays.asList(groupFilters));
        }
        final MemberGroup[] groups = EntityHelper.references(MemberGroup.class, params.getGroupIds());
        if (groups == null || groups.length > 0) {
            query.setGroups(Arrays.asList(groups));
        }
        queryHelper.fill(params, query);
        query.setWithImagesOnly(params.isWithImagesOnly());
        query.setCustomValues(fieldHelper.toValueCollection(CustomField.Nature.MEMBER, params.getFields()));
        return query;
    }

    @SuppressWarnings("unchecked")
    public Member toMember(final RegisterMemberParameters params) {
        // Find the group
        MemberGroup group;
        try {
            final ServiceClient client = fetchService.fetch(WebServiceContext.getClient(), ServiceClient.Relationships.MANAGE_GROUPS);
            final Set<MemberGroup> manageGroups = client.getManageGroups();
            final Long groupId = params.getGroupId();
            if (groupId == null || groupId.intValue() <= 0) {
                group = manageGroups.iterator().next();
            } else {
                group = (MemberGroup) groupService.load(groupId);
                if (!manageGroups.contains(group)) {
                    throw new Exception();
                }
            }
        } catch (final Exception e) {
            throw new EntityNotFoundException();
        }

        final MemberUser user = new MemberUser();
        user.setUsername(params.getUsername());
        final Member member = new Member();
        member.setGroup(group);
        member.setUser(user);
        member.setName(params.getName());
        member.setEmail(params.getEmail());
        List<MemberCustomField> fields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        fields = CustomFieldHelper.onlyForGroup(fields, group);
        final Collection<MemberCustomFieldValue> fieldValues = fieldHelper.toValueCollection(fields, params.getFields());
        member.setCustomValues(fieldValues);
        return member;
    }

    public MemberQuery toQuery(final MemberSearchParameters params) {
        if (params == null) {
            return null;
        }
        final MemberQuery query = new MemberQuery();
        query.setEnabled(true);
        query.fetch(Element.Relationships.GROUP, Element.Relationships.USER);
        if (params.isShowCustomFields()) {
            query.fetch(Member.Relationships.CUSTOM_VALUES);
        }
        if (params.isShowImages()) {
            query.fetch(Member.Relationships.IMAGES);
        }
        final GroupFilter[] groupFilters = EntityHelper.references(GroupFilter.class, params.getGroupFilterIds());
        if (groupFilters == null || groupFilters.length > 0) {
            query.setGroupFilters(Arrays.asList(groupFilters));
        }
        final MemberGroup[] groups = EntityHelper.references(MemberGroup.class, params.getGroupIds());
        if (groups == null || groups.length > 0) {
            query.setGroups(Arrays.asList(groups));
        }
        query.setUsername(params.getUsername());
        query.setName(params.getName());
        query.setEmail(params.getEmail());
        query.setWithImagesOnly(params.isWithImagesOnly());
        query.setRandomOrder(params.isRandomOrder());
        queryHelper.fill(params, query);
        final List<FieldValueVO> fields = params.getFields();
        if (fields != null && fields.size() > 0) {
            query.setCustomValues(fieldHelper.toValueCollection(CustomField.Nature.MEMBER, fields));
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    public MemberResultPage toResultPage(final AbstractMemberSearchParameters params, final List<Member> members) {
        final MemberResultPage page = new MemberResultPage();
        if (members instanceof Page) {
            final Page<Member> memberPage = (Page<Member>) members;
            page.setCurrentPage(memberPage.getCurrentPage());
            page.setTotalCount(memberPage.getTotalCount());
        }
        final List<MemberCustomField> allFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        final Map<MemberGroup, List<MemberCustomField>> fieldsByGroup = new HashMap<MemberGroup, List<MemberCustomField>>();
        final List<MemberVO> vos = new ArrayList<MemberVO>();
        for (final Member member : members) {
            final MemberGroup memberGroup = member.getMemberGroup();
            List<MemberCustomField> fields = null;
            if (params.isShowCustomFields()) {
                fields = fieldsByGroup.get(memberGroup);
                if (fields == null) {
                    fields = CustomFieldHelper.onlyForGroup(allFields, memberGroup);
                    fieldsByGroup.put(memberGroup, fields);
                }
            }
            final MemberVO vo = toVO(member, fields, params.isShowImages());
            if (vo != null) {
                vos.add(vo);
            }
        }
        page.setMembers(vos);
        return page;
    }

    /**
     * Converts a member to VO, with minimum details
     */
    public MemberVO toVO(final Member member) {
        return toVO(member, null, false);
    }

    /**
     * Converts a member to VO
     */
    public MemberVO toVO(final Member member, final List<MemberCustomField> fields, final boolean useImages) {
        if (member == null) {
            return null;
        }
        final MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setName(member.getName());
        vo.setUsername(member.getUsername());
        vo.setEmail(member.getEmail());
        vo.setGroupId(member.getGroup().getId());
        if (fields != null) {
            final Collection<MemberCustomFieldValue> customValues = fetchService.fetch(member, Member.Relationships.CUSTOM_VALUES).getCustomValues();
            vo.setFields(fieldHelper.toList(fields, customValues));
        } else {
            final List<FieldValueVO> empty = Collections.emptyList();
            vo.setFields(empty);
        }
        if (useImages) {
            vo.setImages(imageHelper.toVOs(member.getImages()));
        }
        return vo;
    }
}
