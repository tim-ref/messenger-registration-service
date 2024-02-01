<#ftl output_format="plainText">
<#assign username=user.getUsername()>

${msg("executeActionsBody",link, linkExpiration, username, linkExpirationFormatter(linkExpiration))}
