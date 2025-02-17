import {mount, RouterLinkStub} from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import App from '@/App.vue';
import matomoUtils from "../../src/mixins/matomoUtils";
import displayWrapper from '@/components/displayWrapper.vue';
import store from '@/store';
import { createI18n } from "vue-i18n";
import translations from "@/translations";
import { $gtag } from '../fixtures/testHelpers';


let wrapper;
describe('matomoUtils', () => {
    const i18n = createI18n({
        locale: 'en',
        fallbackLocale: 'en',
        messages: translations
    });

    let router = createRouter({
        history: createWebHistory(process.env.BASE_URL),
        routes: [
            {
                path: '/record/:uuid',
                name: 'displayRecords',
                component: displayWrapper
            }
        ]
    });

    const matomoSetup = jest.spyOn(matomoUtils.methods, 'matomoSetup');

    beforeEach(() => {
        const div = document.createElement('div');
        div.id = 'root'
        div.appendChild(document.createElement('script'));
        document.body.appendChild(div);

        jest.resetAllMocks();
    });

    it("loads the matamo script", () => {
        wrapper = mount(App, {
            attachTo: '#root',
            global: {
                plugins: [router, store, i18n],
                mocks: { $gtag },
                stubs: {
                    RouterLink: RouterLinkStub
                }
            }
        });

        expect(matomoSetup).toHaveBeenCalled();
    });
});