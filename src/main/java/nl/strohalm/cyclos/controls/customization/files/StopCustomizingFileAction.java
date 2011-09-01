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
package nl.strohalm.cyclos.controls.customization.files;

import java.io.File;
import java.io.FileNotFoundException;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile;
import nl.strohalm.cyclos.services.customization.CustomizedFileService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomizationHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.io.FileUtils;
import org.apache.struts.action.ActionForward;

/**
 * Action used to stop customizing a file
 * @author luis
 */
public class StopCustomizingFileAction extends BaseAction {

    private CustomizedFileService customizedFileService;

    public CustomizedFileService getCustomizedFileService() {
        return customizedFileService;
    }

    @Inject
    public void setCustomizedFileService(final CustomizedFileService customizedFileService) {
        this.customizedFileService = customizedFileService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final StopCustomizingFileForm form = context.getForm();
        final long id = form.getFileId();
        if (id <= 0L) {
            throw new ValidationException();
        }

        final CustomizedFile file = customizedFileService.load(id);

        String originalContents = null;
        if (file.isConflict()) {
            originalContents = file.getNewContents();
        } else {
            originalContents = file.getOriginalContents();
        }
        customizedFileService.stopCustomizingGlobal(file);

        final ServletContext servletContext = getServlet().getServletContext();
        final CustomizedFile.Type type = file.getType();
        final File customized = CustomizationHelper.customizedFileOf(servletContext, type, file.getName());
        final File original = CustomizationHelper.originalFileOf(servletContext, type, file.getName());
        switch (type) {
            case APPLICATION_PAGE:
                original.delete();
                original.createNewFile();
                FileUtils.writeStringToFile(original, originalContents);
                break;
            case STYLE:
                // For style sheet files, we must copy the original back.
                customized.getParentFile().mkdirs();
                try {
                    FileUtils.copyFile(original, customized, false);
                } catch (final FileNotFoundException e) {
                    // Ignore - probably a file which no longer exists
                }
                break;
            default:
                // Remove the physical file
                customized.delete();
                break;
        }

        context.sendMessage("customizedFile.removed");
        return ActionHelper.redirectWithParam(context.getRequest(), context.getSuccessForward(), "type", file.getType());
    }

}
