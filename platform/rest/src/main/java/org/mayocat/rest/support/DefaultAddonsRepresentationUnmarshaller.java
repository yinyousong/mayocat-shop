/*
 * Copyright (c) 2012, Mayocat <hello@mayocat.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mayocat.rest.support;

import java.util.List;

import javax.inject.Inject;

import org.mayocat.addons.api.representation.AddonRepresentation;
import org.mayocat.addons.model.AddonField;
import org.mayocat.addons.model.BaseProperties;
import org.mayocat.addons.util.AddonUtils;
import org.mayocat.configuration.PlatformSettings;
import org.mayocat.context.WebContext;
import org.mayocat.model.Addon;
import org.mayocat.model.AddonFieldType;
import org.mayocat.model.AddonSource;
import org.xwiki.component.annotation.Component;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * @version $Id$
 */
@Component
public class DefaultAddonsRepresentationUnmarshaller implements AddonsRepresentationUnmarshaller
{
    @Inject
    private PlatformSettings platformSettings;

    @Inject
    private WebContext context;

    @Override
    public List<Addon> unmarshall(List<AddonRepresentation> addonRepresentations)
    {
        return unmarshall(addonRepresentations, false);
    }

    @Override
    public List<Addon> unmarshall(List<AddonRepresentation> addons, boolean includeReadOnlyAddons)
    {
        return unmarshall(addons, includeReadOnlyAddons, false);
    }

    @Override
    public List<Addon> unmarshall(List<AddonRepresentation> addonRepresentations, boolean includeReadOnlyAddons,
            boolean includeAddonsWithoutDefinition)
    {
        if (addonRepresentations == null) {
            return null;
        }
        List<Addon> addons = Lists.newArrayList();
        for (AddonRepresentation addonRepresentation : addonRepresentations) {
            Addon addon = new Addon();
            addon.setSource(AddonSource.fromJson(addonRepresentation.getSource()));
            addon.setType(AddonFieldType.fromJson(addonRepresentation.getType()));
            addon.setValue(addonRepresentation.getValue());
            addon.setKey(addonRepresentation.getKey());
            addon.setGroup(addonRepresentation.getGroup());

            Optional<AddonField> definition = findAddonDefinition(addon);
            if (definition.isPresent()) {
                if (definition.get().getProperties().containsKey(BaseProperties.READ_ONLY)) {
                    // Read-only addon.
                    // Add to list only if asked for
                    if (includeReadOnlyAddons) {
                        addons.add(addon);
                    }
                } else {
                    addons.add(addon);
                }
            } else {
                // Definition not found
                if (includeAddonsWithoutDefinition) {
                    addons.add(addon);
                }
            }
        }
        return addons;
    }

    private Optional<AddonField> findAddonDefinition(Addon addonToFind)
    {
        Optional option;

        // 1. Find in platform
        option = AddonUtils.findAddonDefinition(addonToFind, platformSettings.getAddons());

        if (!option.isPresent() && context.getTheme() != null) {
            // 2. Find in theme
            option = AddonUtils
                    .findAddonDefinition(addonToFind, context.getTheme().getDefinition().getAddons());
        }

        return option;
    }
}
