<#outputformat "plainText">
    <#assign username=user.getUsername()>
</#outputformat>

<#import "template.ftl" as layout>
<@layout.emailLayout>
    ${kcSanitize(msg("executeActionsBodyHtml",link, linkExpiration, username, linkExpirationFormatter(linkExpiration)))?no_esc}
</@layout.emailLayout>
