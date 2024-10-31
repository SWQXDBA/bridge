import {HttpClient} from "./HttpClient";
<#list apiList as api>
import {${api.clientClassName}} from "./${api.clientClassName}";
</#list>
export class Apis{

    private httpClient:HttpClient

<#list apiList as api>
    ${api.clientClassName}:${api.clientClassName}
</#list>

    public constructor(httpClient:HttpClient){
        this.httpClient = httpClient
    <#list apiList as api>
        this.${api.clientClassName} = new ${api.clientClassName}(httpClient)
    </#list>
    }

}