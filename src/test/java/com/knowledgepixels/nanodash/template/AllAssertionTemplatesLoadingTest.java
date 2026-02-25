package com.knowledgepixels.nanodash.template;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AllAssertionTemplatesLoadingTest {

    /**
     * All 195 published assertion template URIs as of 2025-02-25.
     *
     * Retrieved from the Nanobench SPARQL API:
     *   https://query.petapico.org/api/RA6bgrU3Ezfg5VAiLru0BFYHaSj6vZU6jJTscxNl8Wqvc/get-assertion-templates
     *
     * To refresh this list: fetch the URL above, extract all "np" values, paste them here,
     * then run printLoadingOverview() to find any newly broken templates and remove them.
     */
    static Stream<String> allTemplateUris() {
        return Stream.of(
            "https://w3id.org/np/RAPGpEZJ4__5FKspY1G_xs3cAa6aTGfZFLk81dD_YOEN8",
            "https://w3id.org/np/RArtGIjoFPHB1qumjOhXqDMFYhhzp0JhxjoVb3nB6CiBg",
            "https://w3id.org/np/RAA7AISh2CGalNICIdCpQPspsaQWgaETKOdRlD9e33BC0",
            "https://w3id.org/np/RAMq2mp1s0WfoRYq8Va9-uBCee11Vif78e0o9LKjIRDlI",
            "https://w3id.org/np/RANuZIbOsNsgyfwUva6gG1avJETRxcql8hMsqe5XYyo3Y",
            "https://w3id.org/np/RAHYCMDKC5xYooIF_bjMMpHHQEavF07neVexLxkQ7GmDc",
            "http://purl.org/np/RAx2PsXNbCcxYh3sOSScV9H0-tqyETuKjyHsgD6FPC3_E",
            "https://w3id.org/np/RA1N11KrGR8__rkuQzOpc58vagNlnjIoFlHeaLw_eWDh8",
            "https://w3id.org/np/RAZg-r7oQjVZ3Ewy7pUzd9eINl6fCa3HGclTsDeRag5to",
            "https://w3id.org/np/RAe0zantvnJlVWIC2LueG1IAMktXGFIqCdWliok1rOrmU",
            "https://w3id.org/np/RAwPPxDxkXwgWwYhmvzi6SUs8djPZS4IgWJdp2G0blqoQ",
            "https://w3id.org/np/RAQIZQdqa_kqYfFSnboJOHst-BTcR7_PPG_ywE5EMUEMI",
            "https://w3id.org/np/RAbZPGSnmbAny0ybmvIfpL23e574V3b5JY5xCqKFO1NMw",
            "https://w3id.org/np/RAd3xrwYCOsVgn1S2Ja2knyQBcDy0BHqRIN30ChyhZkcI",
            "https://w3id.org/np/RABLkA6ztDEM4CDwwTCDlSRh01h00FIx2zPF6ueqxB62A",
            "http://purl.org/np/RAZhLP7PiTBTSNhJlRRQT5xPkp___jXP_zC3_yQgkhKhk",
            "https://w3id.org/np/RA88tb2ths5RVTsT7cajUFPL1ONBbyfjk3THDe79FymQA",
            "https://w3id.org/np/RAWulyuueH2y07vOc_4LorkCPSuPe43q4Bnw2rM30Fk0o",
            "https://w3id.org/np/RAiPm1EFNIu-DP4hJkVXvbUiyEFIPI1W5Fj8mXSRaoEQQ",
            "https://w3id.org/np/RARLsTlqbTesu1b0WJZ-zL1z96xumOiqbK3l_vV6iZoww",
            "https://w3id.org/np/RAs3LMTf4JLXDUGCi1MjT2448aJQE3aatSgkapNwgdgHY",
            "https://w3id.org/np/RAbUlt5-fuia6OF436zhxOyq9ZTgSuS1cdkq-PwNsmRs8",
            "https://w3id.org/np/RA8a6AuDKWpWEbUcjwJCFclezi1eMjjn5QdmTLu9PX6OQ",
            "https://w3id.org/np/RATYsQSQd6vWUdtDyIeq7IJg1aG3MiYO0kVD_jUT4CsaI",
            "https://w3id.org/np/RAbn04KkfbV5PK2UDGkp-j7RUghs_y75DL4qWl_8zQQ3w",
            "http://purl.org/np/RA62Ozy4T4_9_JXRCUQOPBkysmvxYOWNjzErxGY5c7zyk",
            "https://w3id.org/np/RASDwdz5WzsWdeDRREs0i5n5xTtKTrKS8pCIFUXhlkpt0",
            "https://w3id.org/np/RAebS74E8oJUknILw57oeL-dA6PNmnbLXxNm-dtNrxftI",
            "https://w3id.org/np/RAA7Z7-LTgnxC6I5bFFJYMK2jzZ2iAzwX_iJe6S1LGMDU",
            "https://w3id.org/np/RAQhbrrzMjfrEk_9s2TurwW4eqKe_ntioqjOq1fI3_X5Q",
            "https://w3id.org/np/RAmw2sp_oDePsdYg-7C8GCTMSzqAG2LFDe6NdRK0FwnQE",
            "https://w3id.org/np/RA_GErDKQT4g0I980hQ0Di1fNHFL3SwvV-IEjJOSEFsGs",
            "https://w3id.org/np/RA6UsUFyh9JUoZHPYSnMiynlUuznP-6svxbjulZRr9L3Q",
            "https://w3id.org/np/RAkRVTW0LAFI1Gfz6BlLwaitVX-I4uPzbGLoGlQrl2-C0",
            "https://w3id.org/np/RA-dNJd-PbNCZPaF1cu-et0piotpnOfpMpNSNtLWXLXzM",
            "https://w3id.org/np/RAmR-xqMgOq3oTJmOVDQFL2p5usID6zqRapizHy0UJb04",
            "https://w3id.org/np/RA77ZfMYXabJMTE_xRvipGAWm0UioetmE80EgUk63W-pQ",
            "https://w3id.org/np/RAReTtpVab4nzaWAstCY74_DWWU6c-gbm0cxgVrrsxFRQ",
            "http://purl.org/np/RAUXTKJMABL75eHuEH5WEw4NweGUKL8gs0v3qot5Tptdo",
            "https://w3id.org/np/RAgtr9qUxGXG_7DNHDRew4B6ZVNpV7La3nKIshMn54OzE",
            "https://w3id.org/np/RAh4iIKHSi30apMADmsYrdyeTd1hvvYZaRRfsKLZX1jsw",
            "https://w3id.org/np/RAShTrZerOrySq-E9DNs6AY6ZbhTVBjxHzoKj_8K2t0HE",
            "https://w3id.org/np/RAvni_QWl93f8eIj6RYA5xO8u5s5stgt4D27_zj4u97sU",
            "https://w3id.org/np/RAlHHOxR1dTkUiDCS-JOf2ZHZdeBfi_voGPcyYPEyxb1k",
            "https://w3id.org/np/RAg7Mnm-tF8Fdktjfs8ST6oZ_Tuf8_8upBcDdFxYEq5yU",
            "https://w3id.org/np/RAb0MsnGztDVu1jCHnvAYVhze-1hhT3zXil0P8trZjPSE",
            "https://w3id.org/np/RAPG8eHxvbsoy2ykHc_HwfSfJLhe-E2AlkOBeNKEbaa3Y",
            "https://w3id.org/np/RAmkA2yJoYZxVIzZ4Rnf2ShrrNBCB_nfjsvV5xHwU3Cuo",
            "https://w3id.org/np/RAsnyFmX6Dc2n4Oxwau33nXJSaXIaU-iGQKVz_FdVqpaU",
            "https://w3id.org/np/RAqDrGVTwvekKoLbwPo5X1SCdB4tiu238_LrCVU9WpARg",
            "https://w3id.org/np/RAwjxSZ6sALIQDJs4LN5uSyFxJgs9j9KYCasPa15MSOZE",
            "https://w3id.org/np/RAadceLO9eTvnfdmuWKiTYLmVLyDevpITnqaJtQW2DnVY",
            "https://w3id.org/np/RA-Eb56zIMZDI6xmeIlFWnq9cm7WdN66A_fz1LvMFVa10",
            "https://w3id.org/np/RAQ1LrU5fyY0Szd-4i9nN_JXBMvKti_v-GeHZgQG-EpzI",
            "https://w3id.org/np/RAZxpqtS3Coiz_fTwsA7n8DnevgIm_7zAv2hWQG_l6rXE",
            "https://w3id.org/np/RATltV_HfIkO1xPPS1iT8e9gS1ac4QgC9qXB90-3ZJH8w",
            "https://w3id.org/np/RA3MNdcfOwPpTdX3rhG-07i9dwQnQIZ3k4pRSSEjJZ1nc",
            "https://w3id.org/np/RAkcfj9W_lJjlq26paIFmTY4mZoaY27BnZCjcsL34EPIA",
            "https://w3id.org/np/RAYXmR56YAlwKU-8h9cITx_0Y1_9t-lqXyVcJAgeEHDjw",
            "https://w3id.org/np/RAa_UkHBTROPhPx0VdLBIBvwFyrQMoaeJ7quriZluksXM",
            "https://w3id.org/np/RAuVB37yyAuAlgusrUAoG84JI4_EfrEqIkpEZYDpSz3d8",
            "https://w3id.org/np/RAR1oWGaiVkWtDL6SdXYnlRBR7t5NoVNa46ShNLJ697g0",
            "https://w3id.org/np/RAMwJK-KwLZao9NyIJ3QPE6AK_MasTNoHlp-PoUEavR2c",
            "https://w3id.org/np/RArfO1jDTJ3CEvLpUz6kYoov_Ziq3J3jCrhmEOy0pHJ3Y",
            "https://w3id.org/np/RAS1RjfEPrerX54sLiZvNyQsGIS5lzF9OgDI3WaqFwxAk",
            "https://w3id.org/np/RA5D3Nk4nNZJV62CDijt8wPsGIbSdQvYxBAWZFhrTE6g4",
            "https://w3id.org/np/RAr8odwaHXvlXco7M3a5iIxyjyfnBxdmlpkoW8m8N8C50",
            "https://w3id.org/np/RAZqKZpanfTaQzMdI4Xq-2Rcpw6pFAO8nt5sT6BTQJKuw",
            "https://w3id.org/np/RA8MyCoRqMdgGqqOwN4MIQfe6Htwt5FPgiHXlXK4RKiic",
            "https://w3id.org/np/RAwQj3SNiopwPrHXfoRT2JtYZSt-5JsDHjBDW6nYz_rDE",
            "https://w3id.org/np/RAivw_N13pxVoXRMP6Y3ErfA--Z011qMqwKccfiKVxF0w",
            "https://w3id.org/np/RAV_H3udaSzxYOhhR0t-q7PKS6URwauD_Z5sMLbHmM2x0",
            "https://w3id.org/np/RAtIPKxhJ69UmpZMO_E8GQdvlhGB6KyVFq9RGGLL70oIM",
            "https://w3id.org/np/RAobILtlfF7HQuIPF_Qo2R09OX0lFyrVSQpVLObBhYuwI",
            "https://w3id.org/np/RAvcJKm2DZPEKOBevGdPGcKL6sEw04JXwgzx2lH5DE5LU",
            "https://w3id.org/np/RAkZ5aKVkobNvvtOvattwuTXlvI0eg38ngzs01m9447Eg",
            "https://w3id.org/np/RArdgzo9AbYj1-Kj3FMaf-P53t9BB-nEobdHK-2GwAdnE",
            "https://w3id.org/np/RA5e5XeXy_-aNK5giB7kBAEQslTLVydHeM4YYEzhmEE2w",
            "https://w3id.org/np/RAxnhKtopKXeNotK-uejPSrZ4EC7sSRw0n3JCAsbzJ-3M",
            "https://w3id.org/np/RAtgc4BBMRZYFbDJ_fSXfGfs2r-kS2DdPvNY_9FWsJjf4",
            "https://w3id.org/np/RA9_MDt_BeNkAvLf1ZqUky5aaNWAGZPB1DcupXwOS3UQE",
            "https://w3id.org/np/RAtUzHSbltSLqUIlPEd6Mpr1Io1KV2L-RFkzVAQYU--Rs",
            "https://w3id.org/np/RAOYgmgXWTKBYDCyJQQH3thfJEiJwmtH75phh1T6Dmk_Q",
            "https://w3id.org/np/RA7vwC0g_WOMKyCy_e0gV97PUgZ9ehZ5kaIZh-dCE9x14",
            "https://w3id.org/np/RAFUH-TQVMTNcQAqShW85pIxWuNNjfyLZgIzoH6v62ddI",
            "https://w3id.org/np/RAFr0RyK-ATWGUnJzKXeRTL0_OrW-mEzoKCaJz0TE8Ao0",
            "https://w3id.org/np/RAYZbhJo8MLTu67DxfBXcf8yXs75IrRfwbajjrQVYCScI",
            "https://w3id.org/np/RAANckrZNdzoNvI6cpz6hv_vscqzXRn522_IsDGMNwEHQ",
            "https://w3id.org/np/RAJJ-AsTOOI_wTej2Taj0ZaZ4janKXJ7akQvanUNGxVRM",
            "https://w3id.org/np/RAs4tQUnXOcDHV6yepMIqj4fmaQmxVkeXCAR85k6dP9Uo",
            "https://w3id.org/np/RA-b4Z62nHyJw6ZzPjIueRzSkLFXu-W76LAp7um8jDWKw",
            "https://w3id.org/np/RAIpUGQSPK-jb-1hJwlAMJmze-VdnOUzHYZ4de7qpUkMQ",
            "https://w3id.org/np/RAXMEZuzjwwx83dyTZp4KB5hyo2kC_nDgsSD9aMWxMYhI",
            "https://w3id.org/np/RA3NvhifSQX43uwcRq_9FxPx51uIaQ_t_qpgkTZRTl1IA",
            "https://w3id.org/np/RAyy-WfI7SCOIB9uuNNjQ_01J4jQaaPKUUMtWugA6e0IU",
            "https://w3id.org/np/RACSI8xZDAn9MUIarbVIMlUBD-_2QiHxdJH59SN6r1tYY",
            "http://purl.org/np/RAQP3NJvnLA2Z-2DrYAN0nTC-RFp67td1t4-pQqQ_ZKmo",
            "https://w3id.org/np/RA4eg0fGov3swvzHmDnKvDnydNezNwCH9g6uPsA9GJ2Mo",
            "https://w3id.org/np/RAoTD9eDmTq9jPCCITjF8mJhMZhBiRD1XoqG4c6U_-dXI",
            "https://w3id.org/np/RAbsAf5-5wnGJB2lSP-wvuAOu_2gBZbK1lp1G9lATTjkk",
            "https://w3id.org/np/RA4dPlp6qcR9SVdxYaXgNYyl_rDRHAEb_fP0Kxo-wDpIU",
            "https://w3id.org/np/RAQj7UGqhY3MsIWMNZc0WdPrzGcLWyYKksj7mUUsbqlzQ",
            "https://w3id.org/np/RAWMq5ryR9pz2Nupqfjx_V9lbpCwmeuoLG0gC6mh4qRAo",
            "https://w3id.org/np/RARBzGkEqiQzeiHk0EXFcv9Ol1d-17iOh9MoFJzgfVQDc",
            "https://w3id.org/np/RAa6LjyInPU_tA7FDZAdVLraBgpd5BIBK59Pmftnc6sYA",
            "https://w3id.org/np/RASr25r0GxVUYGpsJjx3f8y9JE2PzWJnLusIY8XdysKB8",
            "https://w3id.org/np/RAjab0HHFtDtv1h93tF-NCe8VrRFH6KBl_rWbaFqXvEWo",
            "https://w3id.org/np/RAUdNu_If3W3gv3tEpy0DAIMwoZBTQriqiNn4b_QB8SV8",
            "https://w3id.org/np/RA2YwreWrGW9HkzWls8jgwaIINKUB5ZTli1aFKQt13dUk",
            "https://w3id.org/np/RAWV2vOCOfJ2qoeejTujjpx5SzT9uT7shWeKkvGDp92Tc",
            "https://w3id.org/np/RA4tAK_kiFzq5so9wz6_TOwGxhA-eT8EjSZiRk2XmvsdM",
            "https://w3id.org/np/RAuLESdeRUlk1GcTwvzVXShiBMI0ntJs2DL2Bm5DzW_ZQ",
            "https://w3id.org/np/RAKfiqAsawR-NPA3a-7cl7y-XjEAnUVn0hoZd2BVblYe0",
            "https://w3id.org/np/RA5gFoRkA3_i8qpoC7-1iqyHF4SNQj84x2UEwuUfhdo94",
            "https://w3id.org/np/RAe58RLHs0Fvj_fEBMArzAj3OOK7A1qQqWzBQNaLeY9VI",
            "https://w3id.org/np/RAuIQw2ihemTW8JoIUa2mL8pFboITQtNQE4RWnhJUizpI",
            "https://w3id.org/np/RAsOQ7k3GNnuUqZuLm57PWwWopQJR_4onnCpNR457CZg8",
            "https://w3id.org/np/RA-GEms7ZpQableysrmNR6H4UbuzCHNI1_qmurXwjUp_M",
            "https://w3id.org/np/RA9V6mmKNmSGUYJyQ0ahX0fK7su--kK0hmxPlEBoBDpSM",
            "https://w3id.org/np/RAOzG-2x3WDPiygt3P1H2AJMp_Qg34-IhKNNLM6cIlThI",
            "https://w3id.org/np/RAgwb9qyqtuih5_Jypw81ZnnzgZXiRgZ-DnRy3TwV8koo",
            "https://w3id.org/np/RAtqK_UhygrmWHd6QnhZfFboCo6aCsnnfeKIwDsvb9wFQ",
            "https://w3id.org/np/RAzjpzecCuvbkzeiCdNi-6R81aSsTjSDcUM2Lr8eeA-Ps",
            "https://w3id.org/np/RAGsRYGs2XtEL8Lm1emlDMK66HTylM-n3CuzhBtJkWAXQ",
            "https://w3id.org/np/RArj5y6tNzXskSPaa_wl3qdZCGm7d6t0hdOWwpl1Klw6c",
            "https://w3id.org/np/RAr5vrHCpAFxQEKgm0Gpk4YbCg7TH6O7aEU1PqvKzNETc",
            "https://w3id.org/np/RAHYJfXNnuR2q4S5obKbLYiTPVRF9HcqI5_0sCkhRCYNA",
            "https://w3id.org/np/RAxSFpQCJtcy6CT0-F2m7dS3QyR0i-CzsxptxUA3jDX3A",
            "https://w3id.org/np/RAYzJ0kBpauXhQEcuuLmJ2ErOWkcOnzFAaXVp4nPPVq3Y",
            "https://w3id.org/np/RALkWD1XyLSChKe4oWt3m82R6DG839_ILqsosBlFtspkY",
            "https://w3id.org/np/RA-TfgTwRNCcTgGrzSL5iIuS1bOFDBToADPPH-GP7xWA4",
            "https://w3id.org/np/RAJbduMbW71ekYhULtjXdE6qeM0-WUt-WAB3HHA1vYPgE",
            "https://w3id.org/np/RAMe8J0B-bSjHaHnuZUkLG4Pc_eb1eqQmrgL4U7xl6WDQ",
            "https://w3id.org/np/RAb4knjE_IfhWFQ21g_RN90ouncP7MPXhTjeTI7GvwfCM",
            "https://w3id.org/np/RAEpMeneCha0ukXSAErCiLqf7fUvp8sgrhcThsz7YID0g",
            "https://w3id.org/np/RANUaQ7vh5nwWYfclFicTUz6YfugGdTes1sPgUzmk9RVI",
            "https://w3id.org/np/RAxUxo3PDFfPMsyZj0JKLLKieMg4BHIiKVzmYW3zCl6Tc",
            "https://w3id.org/np/RAX2Ah3IMZWFfA3U6Opp3QQCDZLcGtiZuKkhaqMy5fKWM",
            "https://w3id.org/np/RAsPVd3bNOPg5vxQGc1Tqn69v3dSY-ASrAhEFioutCXao",
            "https://w3id.org/np/RARV_0zED9JbBn5Oagr02JgVLJDthd03rf7hC5xY9tgmQ",
            "https://w3id.org/np/RA01Q1XsDIBiUGksbrmU36lOgxXpikqmh2qd4j6AORrcw",
            "https://w3id.org/np/RARGWW8DvbD70AX0XBWT_yrSiUX2kJr8VBZ9X8ivxn_yI",
            "https://w3id.org/np/RABdZf6griTve7mP-0LMd_xWrtYnRPOdN5eNYLbGmJO0I",
            "https://w3id.org/np/RA14Wde02-HPVq2gX3V79vv3tyNciITuhiDQ5eapp3fS0",
            "https://w3id.org/np/RAsl5QL2Cv8nRicFAYI0SPZLlkvHtM9u3ZffuGZautBW4",
            "https://w3id.org/np/RAPLUcr7UGP7UEIax0sguZy8F7nBUhgVuJaL9EcfyGQs0",
            "https://w3id.org/np/RAp9MoSyB8YEaRUyhz7pCK2DPZTK8P3s3OTtsVBuA6Vqk",
            "https://w3id.org/np/RAtVYWSQYeyDS1PHMBkM4VvWr-NXg1_EYftQ6UjMhGiKI",
            "https://w3id.org/np/RA_-Px3U0DFwDqo4NDm1pkPzn6HALZ4AMFRoG2PGFWt_I",
            "https://w3id.org/np/RAnq8pasxkYNwyHAZJVVRu8ZFYvD2R4pvh73pUNtCY4e8",
            "https://w3id.org/np/RAe1X7tHAB1QVxzLPeNaP0dr-sAtb3yrZpoH7mkWx3b2Q",
            "https://w3id.org/np/RAbs9nnm_5c6FvY239iNgsSFq0WkCnadzPtgZmI5q27YQ",
            "https://w3id.org/np/RAQrHqqCRGWExCGVdtDLA0dLb3JBN-GtsU_HHc1fvqUNk",
            "https://w3id.org/np/RAeO8J1AKBA5ApkirD6ewp-b0qSCLMEs933HbAuTIsrhM",
            "https://w3id.org/np/RAUp1wQPKzpT-CXpLW2Ch15cr0RQwLzFI7m3bCp9cnJpY",
            "https://w3id.org/np/RAan-MtO0Klrg1_ymbA8uqWzZab-55ZeyI4iHC4YCceno",
            "https://w3id.org/np/RALwVLnRJ4Y6ZkRC-fDiU0E8QB0XfWBRS0ntWyDMh1l8I",
            "https://w3id.org/np/RA6vg0hTAnvNkbmYZXa3LJFo9APQII1hotXePb9abHbMQ",
            "https://w3id.org/np/RAlxVeww5o6RsAzkcaMKjgAjPPaB1IMqqFg06_AzXNFZc",
            "https://w3id.org/np/RAF79ytpSuemKnx4BP_51YC14PWouYcM6Ha4DNbb2nmOE",
            "https://w3id.org/np/RAF3IMmdkDSbqRDLUo902rSuRXwW-p-waNqpRniMJ-qlg",
            "https://w3id.org/np/RAfZfE1gbUtc35W7xT12XTO0ptZwycN2-jj7Jow6COAoQ",
            "https://w3id.org/np/RApkGsJ5WsWCKq2x1ssDb-boPBydjcsP2b4-pmJlJKTiE",
            "https://w3id.org/np/RAZPmn81h2ShR_KHIhzaEhGIR4u2pp2mun1hblsySWvSY",
            "https://w3id.org/np/RASHzNadGbfsMXwLUc8n3vLI4BICS1UCKUpln0W5FtE4I",
            "https://w3id.org/np/RAAFTG612nBBVEMz7Dc_4OPmB3eQOXe7Gs7lYJf2ImhPk",
            "https://w3id.org/np/RA24onqmqTMsraJ7ypYFOuckmNWpo4Zv5gsLqhXt7xYPU",
            "https://w3id.org/np/RATy4IBU-Qyw-F-XLbFFIjgutHx-ZLWlgb_iDW4wVpzGU",
            "https://w3id.org/np/RAPK3IA9rOxFS2n0e7W1JIGH-GWQxdsJIi6kt-DQRBCoo",
            "https://w3id.org/np/RA9K9Yjc5LQvR3f0IYnJe5xuJvimSucLcLo9O5HAMLfGg",
            "https://w3id.org/np/RAFPOnL42jS_Dixoh_r_PtApB_97RH3Va443rlCixBuOo",
            "https://w3id.org/np/RAqU4PxVtAGW0oLYwXcFZcST5axl7Kk2gIYBAaavRwhgU",
            "https://w3id.org/np/RAhuwjVWOJYg9RzPlooD3M8U1Q09oD7yT8_tBitPWx1Fc",
            "https://w3id.org/np/RAbufoLUmcIHVxzsUv43-bLKrmFgDsZdBxl-XVkVBSkkQ",
            "https://w3id.org/np/RAGf0YXa4-INMV2GWGci7jTGMiQX7ijqcFiFeuXlX8_VE",
            "http://purl.org/np/RAQSsQVscfAon7UqdDsU4yCxlp0NlJGI9WqAXqT3NNIek",
            "https://w3id.org/np/RA3VbbujgFVx0LcaBBpKvzH_hG-I87zhuVYCYJW4whBiI",
            "https://w3id.org/np/RAD1ncuyMZ55LXSbgzqrVSs4RVjb9VjmNmrsN1TAvmokc",
            "https://w3id.org/np/RArM5GTwgxg9qslGX-XiQ-KTTUwdoM0KB1YqmT4GqTizA",
            "https://w3id.org/np/RAiTC_0sDVwAxjmIQ3aXotBJ-sdtkcILLTFDfULH61SBU",
            "https://w3id.org/np/RAnSHAFu_HMWIfNsjXAvL8441RQJCbg7Gu0nMTJUM-0sA",
            "https://w3id.org/np/RAu6at0gGoRR49TXoTlvHUouq4I_hCctW5R947_SIw3J4",
            "https://w3id.org/np/RAymbENMDQq3pPdI4TdIlFMqeIT_xwRf4ovVlKe7BgRUk",
            "https://w3id.org/np/RAd9-ZNaS6vLLNXTWBiKZosCnUV1tQyJ6QyXQyhbrGurE",
            "https://w3id.org/np/RA_cWVA7lhcFZUUcBrQ8p9qxysY7yXjlTlirjJgJ3J0IE",
            "https://w3id.org/np/RAFNJCAnFSxX_VvkzEaUPPDrJ8gqYW_LL8GmmgoohQ9ic",
            "https://w3id.org/np/RAyalx7SHXb5nd-rnImrGZfxJthKgzRnqe7S54rJ_ZWV0",
            "https://w3id.org/np/RAQR7UxvY2GFKTb5fiOJBV68xVsxcmyo9sngleVlwR1Dw",
            "https://w3id.org/np/RAWUaeU7x-RBgYIR33mumfOKdwVPZkL254yyCEDIkAOCo",
            "http://purl.org/np/RA8c438YvDtr5A4DfmB1SmvRQoeeR74RzXYVg3HXo4dR4",
            "http://purl.org/np/RAQL814VkQgU8kYgoKONnxSKP2LWk8PJtcv9VC9StbMDA",
            "https://w3id.org/np/RA2A-0ojBbTr2aeXUe2Bq4Fn8VLl5Ddr82fOuegiILGkA",
            "https://w3id.org/np/RAGVrUZm6j9Yf-K-aJ3Sj-teUbfDsbiRluBrQh_7fxSgw",
            "https://w3id.org/np/RA9C0VyZzJ4FF3gV_MzG33WW1cBASboW6XT6yFt-_Bv4I",
            "https://w3id.org/np/RAvqlX6ptMQa94iIj-c6Sair7SEOUQ9spnR2EPR7lfTD4"
        );
    }

    /**
     * Diagnostic test: prints a summary of which templates load successfully.
     * Run this to get the overview; it never fails.
     */
    @Test
    void printLoadingOverview() {
        List<String> uris = allTemplateUris().toList();
        int success = 0;
        int failure = 0;
        StringBuilder failures = new StringBuilder();

        for (String uri : uris) {
            try {
                new Template(uri);
                success++;
            } catch (Exception e) {
                failure++;
                failures.append("\n  FAIL: ").append(uri)
                        .append("\n       ").append(e.getClass().getSimpleName())
                        .append(": ").append(e.getMessage());
            }
        }

        System.out.printf(
            "%n=== Assertion Template Loading Overview ===%n" +
            "  Success: %d / %d%n" +
            "  Failure: %d / %d%n" +
            "  Failed templates:%s%n",
            success, uris.size(), failure, uris.size(), failures
        );
        // No assertion — this test is purely diagnostic
    }

    /**
     * Regression test: each valid template must load without exception.
     * After running printLoadingOverview(), remove or @Disable any URIs that
     * currently fail so this test suite stays green.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allTemplateUris")
    void templateLoadsSuccessfully(String uri) throws Exception {
        Template template = new Template(uri);
        assertNotNull(template);
    }
}
