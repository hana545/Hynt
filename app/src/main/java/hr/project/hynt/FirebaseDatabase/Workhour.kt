package hr.project.hynt.FirebaseDatabase

data class Workhour( val monday: String = "",
                    // val monday2: String = "",
                     val tuesday: String = "",
                    // val tuesday2: String = "",
                     val wednesday: String = "",
                     //val wednesday2: String = "",
                     val thursday: String = "",
                     //val thurstday2: String = "",
                     val friday: String = "",
                     //val friday2: String = "",
                     val saturday: String = "",
                     //val saturday2: String = "",
                     val sunday: String = "",
                     //val sunday2: String = ""
) {
    operator fun get(i: Int): String {
        if (i == 2){
            return monday
        } else if (i == 3){
            return tuesday
        } else if (i == 4){
            return wednesday
        } else if (i == 5){
            return thursday
        } else if (i == 6){
            return friday
        } else if (i == 7){
            return saturday
        } else if (i == 1){
            return sunday
        }
        return monday

    }
}
