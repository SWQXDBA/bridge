import {HttpClient} from "./HttpClient";
<#list importTypes?sort_by("typeRowString") as type>
import {${type.typeRowString}} from "${typeDirImportName}${type.typeRowString}";
</#list>

${comment}
export class ${clientDefineString}{

    private httpClient:HttpClient
    public constructor(httpClient:HttpClient){
        this.httpClient = httpClient
    }

<#list methods?sort_by("methodName") as method>
${method.comment}
    public ${method.methodName}(${method.params}):Promise<${method.returnType}>{
        return this.httpClient.request(${method.url},'${method.httpMethod}',${method.bodyParam},'${method.contentType}')
    }
</#list>
}