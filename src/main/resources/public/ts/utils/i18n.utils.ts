import {idiom} from "entcore";

export class I18nUtils {
    getWithParams = (key: string, params: string[]) : string => {
        let finalI18n = idiom.translate(key);
        for (let i = 0; i < params.length; i++) {
            finalI18n = finalI18n.replace(`{${i}}`, params[i]);
        }
        return finalI18n;
    };

    getWithParam = (key: string, param: string|number) : string => {
        return this.getWithParams(key, [param.toString()]);
    };

    translate = (key: string) : string => {
        return idiom.translate(key);
    }
}