<template>
    <div class="column is-narrow action-btn item-actions">
        <div v-if="restrictedContent && !isLoggedIn" class="column is-narrow item-actions">
            <div class="restricted-access">
                <h2>{{ $t('full_record.restricted_content', { resource_type: recordData.briefObject.type.toLowerCase() }) }}</h2>
                <div v-if="hasGroupRole(recordData, 'canViewOriginals', 'authenticated')" class="actionlink"><a class="button login-link action" :href="loginUrl"><i class="fa fa-id-card"></i> {{ $t('access.login') }}</a></div>
                <div class="actionlink">
                    <a class="button contact action" href="https://library.unc.edu/wilson/contact/"><i class="fa fa-envelope"></i> {{ $t('access.contact') }}</a>
                </div>
            </div>
        </div>
        <div v-if="hasPermission(recordData, 'editDescription')" class="actionlink">
            <a class="edit button action" :href="editDescriptionUrl(recordData.briefObject.id)"><i class="fa fa-edit"></i> {{ $t('full_record.edit') }}</a>
        </div>
        <template v-if="recordData.resourceType === 'File'">
            <template v-if="hasPermission(recordData, 'viewOriginal')">
                <div class="header-button" v-html="downloadButtonHtml(recordData.briefObject)"></div>
                <div class="actionlink">
                    <a class="button view action" :href="recordData.dataFileUrl">
                        <i class="fa fa-search" aria-hidden="true"></i> View</a>
                </div>
            </template>
        </template>
        <div v-if="fieldExists(recordData.briefObject.embargoDate) && !hasPermission(recordData, 'viewOriginal')" class="noaction">
            {{ $t('full_record.available_date', { available_date: formatDate(recordData.briefObject.embargoDate) }) }}
        </div>
    </div>
</template>

<script>
import fileDownloadUtils from '../../mixins/fileDownloadUtils';
import fullRecordUtils from '../../mixins/fullRecordUtils';

export default {
    name: 'restrictedContent',

    mixins: [fileDownloadUtils, fullRecordUtils],

    props: {
        recordData: Object
    }
}
</script>

<style scoped lang="scss">
 .button {
     white-space: normal;
 }

 .restricted-access .actionlink {
     display: block;
 }

 .header-button {
     display: inline;
 }

 @media (max-width: 768px) {
     .actionlink {
         text-align: left;
         margin: auto;
         justify-content: left;
         width: 99%;
     }

     .header-button {
         display: block;
         margin-bottom: 3px;
         text-align: left;
     }
 }
</style>