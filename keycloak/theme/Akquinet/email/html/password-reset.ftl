<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("passwordResetBodyHtml",link, linkExpiration, user.username, linkExpirationFormatter(linkExpiration)))?no_esc}
</@layout.emailLayout>
