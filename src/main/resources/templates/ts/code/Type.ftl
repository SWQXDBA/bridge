<#list needImportTypes as import>
import {${import}} from "./${import}";
</#list>
${comment}
export ${defineTypeModifier} ${typeDefineString} ${extendStr}{

<#if areEnum>
<#list enumMemberLines as enumLine>
    ${enumLine},

</#list>
<#else>
<#list members as member>
${member.comment}
    <#if member.readOnly>readonly </#if>${member.name}? : ${member.typeString} | null;

</#list>
</#if>


}
<#list enumConstants as constants>
export const ${constants.name} = ${constants.content}
</#list>
