import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router';
import pagination from '@/components/pagination.vue'
import displayWrapper from "@/components/displayWrapper.vue";
import routeUtils from '@/mixins/routeUtils.js';
import store from '@/store';

const gallery = 'gallery-display';
const list_display = 'list-display';
let wrapper, router;

describe('routeUtils',  () => {
    beforeEach(async () => {
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
        // Set wrapper using any component that uses routeUtils mixin to avoid test warnings about missing template
        wrapper = shallowMount(pagination, {
            global: {
                plugins: [router, store]
            }
        });
        await router.push('/record/1234');
    });

    afterEach(() => {
        wrapper.vm.$store.dispatch("resetState");
        wrapper = null;
        router = null;
    });

    it("sets default url parameters for browse view if none are given", () => {
        const defaults = {
            rows: 20,
            start: 0,
            sort: 'default,normal',
            browse_type: list_display,
            works_only: false
        };

        let results = wrapper.vm.urlParams();

        expect(results.rows).toEqual(defaults.rows);
        expect(results.start).toEqual(defaults.start);
        expect(results.sort).toEqual(defaults.sort);
        expect(results.browse_type).toBeUndefined();
        expect(results.works_only).toEqual(defaults.works_only);
    });

    it("sets default url parameters for search view if none are given", () => {
        const defaults = {
            start: 0,
            rows: 20,
            sort: 'default,normal',
            facetSelect: wrapper.vm.possibleFacetFields.join(',')
        };

        let results = wrapper.vm.urlParams({}, true);

        expect(results.rows).toEqual(defaults.rows);
        expect(results.start).toEqual(defaults.start);
        expect(results.sort).toEqual(defaults.sort);
        expect(results.facetSelect).toEqual(defaults.facetSelect);
    });

    it("updates url parameters for a browse view", () => {
        let defaults = {
            rows: 20,
            start: 0,
            sort: 'default,normal',
            browse_type: list_display,
            works_only: false
        };

        defaults.types = 'Work';
        let results = wrapper.vm.urlParams({types: 'Work'});

        expect(results.rows).toEqual(defaults.rows);
        expect(results.start).toEqual(defaults.start);
        expect(results.sort).toEqual(defaults.sort);
        expect(results.browse_type).toBeUndefined();
        expect(results.works_only).toEqual(defaults.works_only);
        expect(results.types).toEqual(defaults.types);
    });

    it("updates url parameters for a search view", () => {
        const defaults = {
            start: 0,
            rows: 20,
            sort: 'default,normal',
            facetSelect: wrapper.vm.possibleFacetFields.join(',')
        };

        let results = wrapper.vm.urlParams({anywhere: 'test', start: 0}, true);

        expect(results.rows).toEqual(defaults.rows);
        expect(results.anywhere).toEqual('test');
        expect(results.start).toEqual(0);
        expect(results.sort).toEqual(defaults.sort);
        expect(results.facetSelect).toEqual(defaults.facetSelect);
    });

    it("formats a url string from an object", () => {
        const defaults = {
            rows: 20,
            start: 0,
            sort: 'title,normal',
            browse_type: gallery,
            works_only: false
        };
        let formatted = `?rows=20&start=0&sort=title%2Cnormal&browse_type=${gallery}&works_only=false`;
        expect(wrapper.vm.formatParamsString(defaults)).toEqual(formatted);
    });

    it("updates work type", async () => {
        // Admin units
        expect(wrapper.vm.updateWorkType(false).types).toEqual('Work,Folder,Collection,File');

        // Works only
        wrapper.vm.$router.currentRoute.value.query.works_only = 'true';
        expect(wrapper.vm.updateWorkType(true).types).toEqual('Work,File');

        // All work types
        wrapper.vm.$router.currentRoute.value.query.works_only = 'false';
        expect(wrapper.vm.updateWorkType(false).types).toEqual('Work,Folder,Collection,File');
    });

    it("coerces works only value to a boolean from a string", () => {
        expect(wrapper.vm.coerceWorksOnly(true)).toEqual(true);
        expect(wrapper.vm.coerceWorksOnly('true')).toEqual(true);
        expect(wrapper.vm.coerceWorksOnly(false)).toEqual(false);
        expect(wrapper.vm.coerceWorksOnly('false')).toEqual(false);
    });

    it("removes query parameters", () => {
        wrapper.vm.$router.currentRoute.value.query.works_only = 'true';
        // Remove no parameters
        expect(wrapper.vm.removeQueryParameters([])).toEqual({ works_only: 'true' });
        // Remove parameter
        expect(wrapper.vm.removeQueryParameters(['works_only'])).toEqual({});
    });

    it("determines if a duplicate route error has been throw", () => {
        expect(wrapper.vm.nonDuplicateNavigationError(
            { name: 'NavigationDuplicated', message:'something awful' })).toBe(false);
        expect(wrapper.vm.nonDuplicateNavigationError(
            { name: 'terrible error', message:'Avoided redundant navigation' })).toBe(false);
        expect(wrapper.vm.nonDuplicateNavigationError(
            { name: 'terrible error', message:'something awful' })).toBe(true);
    });
});