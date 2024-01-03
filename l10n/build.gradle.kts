val languages = "cs_CZ,da_DK,de_DE,en_GB,es_ES,es_MX,fi_FI,fr_CA,fr_FR,hu_HU,it_IT,ja_JP,lv_LV,nl_NL,pl_PL,pt_BR,ro_RO,ru_RU,sk_SK,sv_SE,zh_CN,zh_TW".split(",")
val communityLanguages = "ca_ES,el_GR,et_EE,eu_ES,gl_ES,he_HE,hi_IN,ko_KO,nb_NO,pt_PT,th_TH,tr_TR,vi_VI".split(",")

install {
    target("languages") {
        from(projectDir) {
            (languages + communityLanguages).forEach {
                include("backend.${it}.po")
                include("client-onboarding-scenarios.${it}.po")
            }
        }
        into(prefixResolve("i18n"))
    }
}
