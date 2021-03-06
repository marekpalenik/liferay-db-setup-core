package com.ableneo.liferay.portal.setup.core;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Original work Copyright (C) 2016 - 2018 mimacom ag
 * Modified work Copyright (C) 2018 - 2020 ableneo, s. r. o.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.DocumentUtil;
import com.ableneo.liferay.portal.setup.core.util.FolderUtil;
import com.ableneo.liferay.portal.setup.core.util.ResourcesUtil;
import com.ableneo.liferay.portal.setup.domain.Document;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.security.permission.ActionKeys;

public final class SetupDocuments {

    private static final Log LOG = LogFactoryUtil.getLog(SetupDocuments.class);
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS;

    static {
        DEFAULT_PERMISSIONS = new HashMap<>();
        List<String> actionsOwner = new ArrayList<>();

        actionsOwner.add(ActionKeys.ADD_DISCUSSION);
        actionsOwner.add(ActionKeys.DELETE);
        actionsOwner.add(ActionKeys.DELETE_DISCUSSION);
        actionsOwner.add(ActionKeys.OVERRIDE_CHECKOUT);
        actionsOwner.add(ActionKeys.PERMISSIONS);
        actionsOwner.add(ActionKeys.UPDATE);
        actionsOwner.add(ActionKeys.UPDATE_DISCUSSION);
        actionsOwner.add(ActionKeys.VIEW);

        DEFAULT_PERMISSIONS.put(RoleConstants.OWNER, actionsOwner);

        List<String> actionsUser = new ArrayList<>();
        actionsUser.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.USER, actionsUser);

        List<String> actionsGuest = new ArrayList<>();
        actionsGuest.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.GUEST, actionsGuest);
    }

    private SetupDocuments() {

    }

    public static void setupSiteDocuments(final Site site, final long groupId) {
        for (Document doc : site.getDocument()) {
            String folderPath = doc.getDocumentFolderName();
            String documentName = doc.getDocumentFilename();
            String documentTitle = doc.getDocumentTitle();
            String filenameInFilesystem = doc.getFileSystemName();
            long repoId = groupId;
            long userId = SetupConfigurationThreadLocal.getRunAsUserId();
            long company = SetupConfigurationThreadLocal.getRunInCompanyId();
            if (folderPath != null && !folderPath.equals("")) {
                FolderUtil.findFolder(groupId, repoId, folderPath, true);
            }
            FileEntry fe = DocumentUtil.findDocument(documentName, folderPath, groupId, groupId);
            byte[] fileBytes = null;
            try {
                fileBytes = ResourcesUtil.getFileBytes(filenameInFilesystem);
            } catch (IOException e) {
                LOG.error(String.format("Can not read file: %1$s. Skipping file", filenameInFilesystem));
                continue;
            }
            if (fileBytes != null) {
                if (fe == null) {
                    fe = DocumentUtil.createDocument(company, groupId, documentName, documentTitle, userId, repoId,
                            fileBytes);
                    LOG.info(documentName + " is not found! It will be created! ");
                } else {
                    LOG.info(documentName + " is found! Content will be updated! ");
                    DocumentUtil.updateFile(fe, fileBytes, userId, documentName);
                }
                SetupPermissions.updatePermission(String.format("Document %1$s/%2$s", folderPath, documentName),
                        company, fe.getFileEntryId(), DLFileEntry.class, doc.getRolePermissions(), DEFAULT_PERMISSIONS);
            }
        }
    }
}
