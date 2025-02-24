import {RouterLinkStub, shallowMount} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import cloneDeep from 'lodash.clonedeep';
import fileList from '@/components/full_record/fileList.vue';
import displayWrapper from '@/components/displayWrapper.vue';
import {createI18n} from 'vue-i18n';
import translations from '@/translations';

let briefObject = {
    filesizeTotal: 694904,
    format: [
        "Image"
    ],
    title: "beez",
    type: "File",
    fileDesc: [
        "JPEG Image"
    ],
    parentCollectionName: "testCollection",
    datastream: [
        "original_file|image/jpeg|beez||694904|urn:sha1:0d48dadb5d61ae0d41b4998280a3c39577a2f94a||2048x1536",
        "jp2|image/jp2|4db695c0-5fd5-4abf-9248-2e115d43f57d.jp2|jp2|2189901|||"
    ],
    parentCollectionId: "fc77a9be-b49d-4f4e-b656-1644c9e964fc",
    permissions: [
        "viewAccessCopies",
        "viewMetadata"
    ],
    groupRoleMap: {
        authenticated: [
            "canViewAccessCopies"
        ],
        everyone: [
            "canViewAccessCopies"
        ]
    },
    id: "4db695c0-5fd5-4abf-9248-2e115d43f57d",
    fileType: [
        "image/jpeg"
    ],
    status: [
        "Patron Settings",
        "Inherited Patron Settings"
    ]
};

let wrapper, router;

describe('fileList.vue', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    beforeEach(() => {
        router = createRouter({
            history: createWebHistory(process.env.BASE_URL),
            routes: [
                {
                    path: '/record/:uuid',
                    name: 'displayRecords',
                    component: displayWrapper
                }
            ]
        });

        wrapper = shallowMount(fileList, {
            global: {
                plugins: [i18n, router],
                stubs: {
                    RouterLink: RouterLinkStub
                }
            },

            props: {
                childCount: 3,
                editAccess: true,
                viewOriginal: false,
                workId: 'e2f0d544-4f36-482c-b0ca-ba11f1251c01',
            }
        });
    });

    it("displays a header with file count", () => {
        expect(wrapper.find('h3').text()).toEqual("List of Items in This Work (3)");
    });

    it("contains a table of files", () => {
        expect(wrapper.findComponent({ name: 'dataTable' }).exists()).toBe(true);
    });

    it("sets 'badge' options for thumbnails", () => {
        expect(wrapper.vm.showBadge({ status: ['Marked for Deletion', 'Public Access'] })).toEqual({ markDeleted: true, restricted: false });
        expect(wrapper.vm.showBadge({ status: [''] })).toEqual({ markDeleted: false, restricted: true });
    });

    // @TODO TDB whether viewAccessCopies allows a user to download anything
   /* it("sets download button html for image files with canViewAccess permission", () => {
        const download = wrapper.vm.downloadButtonHtml(briefObject);
        // Download button
        expect(download).toEqual(expect.stringContaining('button id="dcr-download-4db695c0-5fd5-4abf-9248-2e115d43f57d"'));
        // Options
        expect(download).toEqual(expect.stringContaining('Small JPG (800px)'));
        expect(download).toEqual(expect.stringContaining('Medium JPG (1600px)'));
        expect(download).toEqual(expect.not.stringContaining('Full Size JPG'));
        expect(download).toEqual(expect.not.stringContaining('Original File'));
    });*/

    it("sets download button html for image files with canViewOriginal permission", async () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.permissions = [
            "viewAccessCopies",
            "viewMetadata",
            "viewOriginal"
        ]
        updatedBriefObj.groupRoleMap = {
            authenticated: ["canViewOriginals"],
            everyone: ["canViewOriginals"]
        };

        await  wrapper.setProps({
            viewOriginal: true
        })

        const download = wrapper.vm.downloadButtonHtml(updatedBriefObj);
        // Download button
        expect(download).toEqual(expect.stringContaining('button id="dcr-download-4db695c0-5fd5-4abf-9248-2e115d43f57d"'));
        // Options
        expect(download).toEqual(expect.stringContaining('Small JPG (800px)'));
        expect(download).toEqual(expect.stringContaining('Medium JPG (1600px)'));
        expect(download).toEqual(expect.stringContaining('Full Size JPG'));
        expect(download).toEqual(expect.stringContaining('Original File'));
    });

    it("sets download button html for non-image files", () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.fileType = ['application/pdf']
        updatedBriefObj.format = ['Text']
        updatedBriefObj.permissions = [
            "viewAccessCopies",
            "viewMetadata",
            "viewOriginal"
        ]
        updatedBriefObj.datastream = ['original_file|application/pdf|pdf file||416330|urn:sha1:4945153c9f5ce152ef8eda495deba043f536f388||'];

        const download = wrapper.vm.downloadButtonHtml(updatedBriefObj);
        // Download button
        expect(download).toEqual(expect.stringContaining('<a class="download button action"'));
    });

    it("sets a disabled button for non-image files without showImageDownload permission", () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.fileType = ['application/pdf']
        updatedBriefObj.format = ['Text']
        updatedBriefObj.datastream = ['original_file|application/pdf|pdf file||416330|urn:sha1:4945153c9f5ce152ef8eda495deba043f536f388||'];

        const download = wrapper.vm.downloadButtonHtml(updatedBriefObj);
        // Disabled download button
        expect(download).toEqual(expect.stringContaining('button class="button download-images" title="Download Unavailable" disabled'));
    });

    it("sets a disabled download button for image files without viewAccessCopies permission", () => {
        let updatedBriefObj = cloneDeep(briefObject);
        updatedBriefObj.permissions = [
            "viewMetadata"
        ];

        const download = wrapper.vm.downloadButtonHtml(updatedBriefObj);
        // Disabled download button
        expect(download).toEqual(expect.stringContaining('button class="button download-images" title="Download Unavailable" disabled'));
    });
});