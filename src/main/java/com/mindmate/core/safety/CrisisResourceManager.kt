package com.mindmate.core.safety

import com.mindmate.domain.model.CrisisResource
import com.mindmate.domain.model.ResourceType

object CrisisResourceManager {

    /**
     * Verified offline emergency and crisis resources in India.
     * Guaranteed to be available without internet access.
     */
    val verifiedResources: List<CrisisResource> = listOf(
        CrisisResource(
            id = "tele_manas",
            name = "Tele-MANAS (Govt of India)",
            description = "24/7 Comprehensive mental health support in English, Telugu, Hindi, and 20+ regional languages. Free and confidential.",
            phone = "14416",
            website = "https://telemanas.mohfw.gov.in",
            availability = "24/7 Toll-Free",
            region = "All India (Govt of India)",
            type = ResourceType.GOVERNMENT_TELE_MANAS,
            verified = true
        ),
        CrisisResource(
            id = "tele_manas_tollfree",
            name = "Tele-MANAS Alternate Toll-Free",
            description = "Alternate 10-digit toll-free number for Tele-MANAS callers.",
            phone = "18008914416",
            website = "https://telemanas.mohfw.gov.in",
            availability = "24/7 Toll-Free",
            region = "All India",
            type = ResourceType.GOVERNMENT_TELE_MANAS,
            verified = true
        ),
        CrisisResource(
            id = "kiran_helpline",
            name = "KIRAN National Helpline",
            description = "Ministry of Social Justice 24/7 mental health rehabilitation helpline.",
            phone = "18005990019",
            website = "https://disabilityaffairs.gov.in",
            availability = "24/7 Toll-Free",
            region = "All India",
            type = ResourceType.NATIONAL_CRISIS,
            verified = true
        ),
        CrisisResource(
            id = "emergency_112",
            name = "National Emergency Number",
            description = "Immediate police, medical emergency, or disaster response in India.",
            phone = "112",
            website = "https://112.gov.in",
            availability = "24/7 Immediate",
            region = "All India",
            type = ResourceType.EMERGENCY_SERVICES,
            verified = true
        ),
        CrisisResource(
            id = "nimhans",
            name = "NIMHANS Psychosocial Helpline",
            description = "Specialized clinical and psychosocial support by premier national institute.",
            phone = "08046110007",
            website = "https://nimhans.ac.in",
            availability = "24/7 Support",
            region = "All India",
            type = ResourceType.NATIONAL_CRISIS,
            verified = true
        ),
        CrisisResource(
            id = "hyderabad_student_support",
            name = "Hyderabad / Telangana Youth Helpline",
            description = "Regional student and youth counseling service for college pressures and emotional support.",
            phone = "04027632688",
            website = "https://telangana.gov.in",
            availability = "9 AM - 9 PM IST",
            region = "Hyderabad & Telangana",
            type = ResourceType.STUDENT_COUNSELING,
            verified = true
        ),
        CrisisResource(
            id = "drug_deaddiction_1972",
            name = "National Drug De-Addiction Helpline",
            description = "Ministry of Social Justice & Empowerment 24/7 confidential counseling, de-addiction guidance, and rehabilitation support.",
            phone = "1972",
            website = "https://socialjustice.gov.in",
            availability = "24/7 Toll-Free",
            region = "All India (Govt of India)",
            type = ResourceType.SUBSTANCE_SUPPORT,
            verified = true
        )
    )

    fun getPrimaryEmergencyResource(): CrisisResource {
        return verifiedResources.first()
    }
}
