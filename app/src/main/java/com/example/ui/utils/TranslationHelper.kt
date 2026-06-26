package com.example.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.data.PreferenceManager

object L10n {
    const val LANG_EN = "English"
    const val LANG_HI = "Hindi (हिन्दी)"
    const val LANG_ES = "Spanish (Español)"

    private val translations = mapOf(
        LANG_EN to mapOf(
            // Tabs
            "home" to "Home",
            "team" to "Team",
            "blog" to "Blog",
            "mine" to "Mine",

            // Home Header
            "balance" to "Balance",
            "bonus" to "Bonus",
            "recharge" to "Recharge",
            "user_id" to "User ID",

            // Home Actions
            "deposit" to "Deposit",
            "withdraw" to "Withdraw",
            "share" to "Share",
            "online" to "Online",

            // Referral card
            "referral_desc" to "Share your referral link and start earning",
            "copy" to "Copy",
            "copied" to "Copied!",

            // Team metrics
            "team_size" to "Team Size",
            "team_rank" to "Team Rank",
            "total_income" to "Total Income",
            "active_team" to "Active Team",
            "total_earning" to "Total Earning",
            "total_registered" to "Total Registered",
            "total_earnings" to "Total Earnings",
            "register_active" to "Register / Active",
            "total_active" to "Total / Active",
            "commission" to "Commission",
            "income" to "Income",
            "details" to "Details",
            "business_volume" to "Business Volume",
            "upgrade_rank_1" to "Upgrade your rank and",
            "upgrade_rank_2" to "earn higher commissions.",
            "view_rank_benefits" to "View Rank Benefits",

            // Investment Tabs
            "fixed_fund" to "Fixed Fund",
            "welfare_fund" to "Welfare Fund",
            "yearly_fund" to "Yearly Fund",

            // Mine screen menus
            "bank_card" to "Bank Card",
            "order_history" to "Order History",
            "fund_history" to "Fund History",
            "gift_code" to "Gift Code",
            "task" to "Task",
            "support" to "Support",
            "settings" to "Settings",
            "admin_console" to "Admin Console",
            "payment_callbacks" to "Payment Callbacks",
            "about_invexx" to "About Invexx",
            "logout" to "Logout",
            "logout_confirm" to "Are you sure you want to log out?",
            "cancel" to "Cancel",

            // Settings Screen
            "preferences" to "Preferences",
            "push_notifications" to "Push Notifications",
            "app_language" to "App Language",
            "security_protection" to "Security & Protection",
            "withdrawal_pin" to "Withdrawal PIN",
            "biometrics_login" to "Biometrics Login",
            "reset_password" to "Reset Account Password",
            "account_verification" to "Account Verification",
            "kyc_verified" to "KYC Identity Verified",
            "full_features" to "Full wallet features unlocked",
            "active" to "Active",

            // Support Screen
            "need_help" to "Need Help? 💬",
            "support_desc" to "Our premium support services are online 24/7. Connect with us directly for fast resolution of your deposit, withdrawal, or system inquiries.",
            "official_support" to "Official Customer Service",
            "telegram_channel" to "Official Telegram Channel",
            "telegram_desc" to "Real-time corporate updates & welfares",
            "whatsapp_hotline" to "VIP WhatsApp Hot-line",
            "whatsapp_desc" to "Fast response for VIP rank holders",
            "support_email" to "Official Support Email",
            "faqs" to "Frequently Asked Questions",

            // About Screen
            "about_title" to "About INVEXX",
            "secure_transparent" to "Secure. Transparent. Future-Proof.",
            "who_we_are" to "Who We Are",
            "who_we_are_desc" to "INVEXX is a leading global investment platform specializing in diversified high-yield financial portfolios. Our mission is to democratize premium wealth generation, allowing retail investors to tap into institutional-grade Fixed Funds, Welfare Funds, and Yearly Funds with institutional-level security and maximum flexibility.",
            "core_pillars" to "Our Core Pillars",
            "pillar_1_title" to "Advanced Security Protocols",
            "pillar_1_desc" to "Your funds and returns are protected by robust escrow networks and secure blockchain transaction registers, ensuring 100% security against unauthorized access.",
            "pillar_2_title" to "Guaranteed Wealth Growth",
            "pillar_2_desc" to "With systematic plans backed by energy assets, commercial technologies, and market-hedged index structures, we ensure your daily dividends are credited like clockwork.",
            "pillar_3_title" to "Dynamic Affiliate Rewards",
            "pillar_3_desc" to "Grow together with our multi-level partner programs. Earn up to 35% in direct deposit commissions and secondary team dividends, boosting your passive income.",
            "app_version" to "INVEXX App Version",
            "rights_reserved" to "© 2026 INVEXX International Ltd. All rights reserved."
        ),
        LANG_HI to mapOf(
            // Tabs
            "home" to "होम",
            "team" to "टीम",
            "blog" to "ब्लॉग",
            "mine" to "प्रोफाइल",

            // Home Header
            "balance" to "कुल बैलेंस",
            "bonus" to "बोनस",
            "recharge" to "रिचार्ज",
            "user_id" to "यूज़र आईडी",

            // Home Actions
            "deposit" to "डिपॉजिट",
            "withdraw" to "निकासी",
            "share" to "शेयर करें",
            "online" to "ऑनलाइन सहायता",

            // Referral card
            "referral_desc" to "अपना रेफ़रल लिंक साझा करें और कमाना शुरू करें",
            "copy" to "कॉपी करें",
            "copied" to "कॉपी हो गया!",

            // Team metrics
            "team_size" to "टीम साइज़",
            "team_rank" to "टीम रैंक",
            "total_income" to "कुल आय",
            "active_team" to "सक्रिय टीम",
            "total_earning" to "कुल कमाई",
            "total_registered" to "कुल पंजीकृत",
            "total_earnings" to "कुल कमाई",
            "register_active" to "पंजीकृत / सक्रिय",
            "total_active" to "कुल / सक्रिय",
            "commission" to "कमीशन",
            "income" to "आय",
            "details" to "विवरण",
            "business_volume" to "बिजनेस वॉल्यूम",
            "upgrade_rank_1" to "अपनी रैंक अपग्रेड करें और",
            "upgrade_rank_2" to "उच्च कमीशन कमाएं।",
            "view_rank_benefits" to "रैंक लाभ देखें",

            // Investment Tabs
            "fixed_fund" to "फिक्स्ड फंड",
            "welfare_fund" to "कल्याणकारी फंड",
            "yearly_fund" to "वार्षिक फंड",

            // Mine screen menus
            "bank_card" to "बैंक कार्ड",
            "order_history" to "ऑर्डर इतिहास",
            "fund_history" to "फंड इतिहास",
            "gift_code" to "गिफ्ट कोड",
            "task" to "कार्य (टास्क)",
            "support" to "सहायता",
            "settings" to "सेटिंग्स",
            "admin_console" to "एडमिन कंसोल",
            "payment_callbacks" to "भुगतान कॉलबैक (Logs)",
            "about_invexx" to "इनवेक्स के बारे में",
            "logout" to "लॉगआउट",
            "logout_confirm" to "क्या आप वाकई लॉग आउट करना चाहते हैं?",
            "cancel" to "रद्द करें",

            // Settings Screen
            "preferences" to "प्राथमिकताएं",
            "push_notifications" to "पुश नोटिफिकेशन्स",
            "app_language" to "ऐप की भाषा",
            "security_protection" to "सुरक्षा और संरक्षण",
            "withdrawal_pin" to "निकासी पिन",
            "biometrics_login" to "बायोमेट्रिक्स लॉगिन",
            "reset_password" to "खाता पासवर्ड रीसेट करें",
            "account_verification" to "खाता सत्यापन (KYC)",
            "kyc_verified" to "KYC पहचान सत्यापित",
            "full_features" to "पूर्ण वॉलेट सुविधाएं अनलॉक हैं",
            "active" to "सक्रिय",

            // Support Screen
            "need_help" to "मदद चाहिए? 💬",
            "support_desc" to "हमारी प्रीमियम सहायता सेवाएं 24/7 ऑनलाइन हैं। अपने डिपॉजिट, निकासी या सिस्टम पूछताछ के त्वरित समाधान के लिए हमसे सीधे जुड़ें।",
            "official_support" to "आधिकारिक ग्राहक सेवा",
            "telegram_channel" to "आधिकारिक टेलीग्राम चैनल",
            "telegram_desc" to "रीयल-टाइम कॉर्पोरेट अपडेट और कल्याण लाभ",
            "whatsapp_hotline" to "वीआईपी व्हाट्सएप हेल्पलाइन",
            "whatsapp_desc" to "वीआईपी रैंक धारकों के लिए त्वरित प्रतिक्रिया",
            "support_email" to "आधिकारिक सहायता ईमेल",
            "faqs" to "अक्सर पूछे जाने वाले प्रश्न (FAQs)",

            // About Screen
            "about_title" to "इनवेक्स के बारे में",
            "secure_transparent" to "सुरक्षित। पारदर्शी। भविष्य-सुरक्षित।",
            "who_we_are" to "हम कौन हैं",
            "who_we_are_desc" to "इनवेक्स एक अग्रणी वैश्विक निवेश मंच है जो विविध उच्च-उपज वित्तीय पोर्टफोलियो में विशेषज्ञता रखता है। हमारा मिशन प्रीमियम धन सृजन का लोकतंत्रीकरण करना है, जिससे खुदरा निवेशक संस्थागत स्तर की सुरक्षा और अधिकतम लचीलेपन के साथ फिक्स्ड फंड, कल्याणकारी फंड और वार्षिक फंड का लाभ उठा सकें।",
            "core_pillars" to "हमारे मुख्य स्तंभ",
            "pillar_1_title" to "उन्नत सुरक्षा प्रोटोकॉल",
            "pillar_1_desc" to "आपके फंड और रिटर्न मजबूत एस्क्रो नेटवर्क और सुरक्षित ब्लॉकचेन लेनदेन रजिस्टरों द्वारा सुरक्षित हैं, जो अनधिकृत पहुंच के खिलाफ 100% सुरक्षा सुनिश्चित करते हैं।",
            "pillar_2_title" to "गारंटीकृत धन वृद्धि",
            "pillar_2_desc" to "ऊर्जा संपत्तियों, वाणिज्यिक तकनीकों और बाजार-हेज्ड इंडेक्स संरचनाओं द्वारा समर्थित व्यवस्थित योजनाओं के साथ, हम सुनिश्चित करते हैं कि आपके दैनिक लाभांश समय पर क्रेडिट हों।",
            "pillar_3_title" to "गतिशील संबद्ध पुरस्कार",
            "pillar_3_desc" to "हमारे बहु-स्तरीय भागीदार कार्यक्रमों के साथ मिलकर बढ़ें। प्रत्यक्ष जमा कमीशन और माध्यमिक टीम लाभांश में 35% तक कमाएं, जिससे आपकी निष्क्रिय आय बढ़ेगी।",
            "app_version" to "इनवेक्स ऐप संस्करण",
            "rights_reserved" to "© 2026 इनवेक्स इंटरनेशनल लिमिटेड। सर्वाधिकार सुरक्षित।"
        ),
        LANG_ES to mapOf(
            // Tabs
            "home" to "Inicio",
            "team" to "Equipo",
            "blog" to "Blog",
            "mine" to "Perfil",

            // Home Header
            "balance" to "Saldo",
            "bonus" to "Bono",
            "recharge" to "Recarga",
            "user_id" to "ID de Usuario",

            // Home Actions
            "deposit" to "Depósito",
            "withdraw" to "Retirar",
            "share" to "Compartir",
            "online" to "Soporte",

            // Referral card
            "referral_desc" to "Comparte tu enlace de referencia y empieza a ganar",
            "copy" to "Copiar",
            "copied" to "¡Copiado!",

            // Team metrics
            "team_size" to "Miembros",
            "team_rank" to "Rango",
            "total_income" to "Ingresos Totales",
            "active_team" to "Equipo Activo",
            "total_earning" to "Ganancia Total",
            "total_registered" to "Total Registrados",
            "total_earnings" to "Ganancias Totales",
            "register_active" to "Registro / Activo",
            "total_active" to "Total / Activo",
            "commission" to "Comisión",
            "income" to "Ingresos",
            "details" to "Detalles",
            "business_volume" to "Volumen de Negocio",
            "upgrade_rank_1" to "Mejora tu rango y",
            "upgrade_rank_2" to "gana mayores comisiones.",
            "view_rank_benefits" to "Ver Beneficios de Rango",

            // Investment Tabs
            "fixed_fund" to "Fondo Fijo",
            "welfare_fund" to "Fondo de Bienestar",
            "yearly_fund" to "Fondo Anual",

            // Mine screen menus
            "bank_card" to "Tarjeta Bancaria",
            "order_history" to "Historial de Pedidos",
            "fund_history" to "Historial de Fondos",
            "gift_code" to "Código de Regalo",
            "task" to "Tareas",
            "support" to "Soporte",
            "settings" to "Ajustes",
            "admin_console" to "Consola de Admin",
            "payment_callbacks" to "Callbacks de Pago",
            "about_invexx" to "Acerca de Invexx",
            "logout" to "Cerrar sesión",
            "logout_confirm" to "¿Estás seguro de que deseas cerrar la sesión?",
            "cancel" to "Cancelar",

            // Settings Screen
            "preferences" to "Preferencias",
            "push_notifications" to "Notificaciones Push",
            "app_language" to "Idioma de la App",
            "security_protection" to "Seguridad y Protección",
            "withdrawal_pin" to "PIN de Retiro",
            "biometrics_login" to "Inicio Biométrico",
            "reset_password" to "Restablecer Contraseña",
            "account_verification" to "Verificación de Cuenta",
            "kyc_verified" to "Identidad KYC Verificada",
            "full_features" to "Funciones de billetera desbloqueadas",
            "active" to "Activo",

            // Support Screen
            "need_help" to "¿Necesitas ayuda? 💬",
            "support_desc" to "Nuestros servicios de soporte premium están en línea 24/7. Conéctese con nosotros directamente para una rápida resolución de sus consultas.",
            "official_support" to "Servicio de Atención al Cliente",
            "telegram_channel" to "Canal Oficial de Telegram",
            "telegram_desc" to "Actualizaciones corporativas y beneficios en tiempo real",
            "whatsapp_hotline" to "Línea Directa VIP WhatsApp",
            "whatsapp_desc" to "Respuesta rápida para miembros VIP",
            "support_email" to "Correo Electrónico Oficial",
            "faqs" to "Preguntas Frecuentes (FAQs)",

            // About Screen
            "about_title" to "Acerca de INVEXX",
            "secure_transparent" to "Seguro. Transparente. A prueba de futuro.",
            "who_we_are" to "Quiénes Somos",
            "who_we_are_desc" to "INVEXX es una plataforma de inversión global líder especializada en carteras financieras diversificadas de alto rendimiento. Nuestra misión es democratizar la generación de riqueza premium.",
            "core_pillars" to "Nuestros Pilares",
            "pillar_1_title" to "Protocolos de Seguridad Avanzados",
            "pillar_1_desc" to "Sus fondos y rendimientos están protegidos por sólidas redes de custodia y registros seguros de transacciones blockchain.",
            "pillar_2_title" to "Crecimiento de Riqueza Garantizado",
            "pillar_2_desc" to "Con planes sistemáticos respaldados por activos energéticos y tecnologías comerciales, aseguramos sus dividendos.",
            "pillar_3_title" to "Recompensas de Afiliados Dinámicas",
            "pillar_3_desc" to "Gane hasta un 35% en comisiones de depósito directo y dividendos de equipos secundarios.",
            "app_version" to "Versión de la aplicación",
            "rights_reserved" to "© 2026 INVEXX International Ltd. Todos los derechos reservados."
        )
    )

    fun getString(key: String, language: String): String {
        val langMap = translations[language] ?: translations[LANG_EN]!!
        return langMap[key] ?: translations[LANG_EN]!![key] ?: key
    }
}

@Composable
fun tr(key: String): String {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager(context) }
    return L10n.getString(key, prefs.appLanguage)
}
