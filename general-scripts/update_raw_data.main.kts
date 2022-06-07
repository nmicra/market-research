// USAGE EXAMPLE
// docker run -e JAVA_OPTS="-Xms1g -Xmx4g" -e RATER_URL="jdbc:postgresql://10.210.150.235:6432/solid_rater" -e AUTH_URL="jdbc:postgresql://10.210.150.235:6432/solid_authorization" -e RATER_DB_USER="solid_application" -e AUTH_DB_USER="solid_application" -e RATER_DB_PASS="srSdKV8G" -e AUTH_DB_PASS="srSdKV8G" -i serandel/kscript - < subscriber.main.kts


@file:DependsOn("org.postgresql:postgresql:42.3.1")
@file:DependsOn("de.sfuhrm:YahooFinanceAPI:3.16.3")

import java.sql.DriverManager
import java.util.UUID
import java.time.LocalDateTime
import java.sql.Timestamp
import java.util.Calendar
import kotlin.system.exitProcess

val jdbcUrl = "jdbc:postgresql://us-west1.ec90e29a-5a4a-48fd-a976-b97c4dead805.gcp.ybdb.io:5433/yugabyte?ssl=true&sslmode=verify-full&sslrootcert=C://gld//yugabyte-ca.crt"
val jdbcUser = "admin"
val jdbcPass = "d6Ja4kbdyOX7MdAmd1A21OM03L_2De"

val LATEST_AVAILIBLE_SQL = """
   SELECT T1.*
        FROM market_raw T1
        WHERE T1.date = (SELECT max(T2.date)
        FROM market_raw T2 where T2.label='GOLD')
         AND T1.label='GOLD'
""".trimIndent()

val connectionAuth = DriverManager.getConnection(jdbcUrl, "$jdbcUser", "$jdbcPass")
    .also { it.autoCommit = false }

println("--- start ${LocalDateTime.now()}")
val isValidAuthConnection = connectionAuth.isValid(0)
println("--- dbConnection=$isValidAuthConnection")


if (!isValidAuthConnection) {
    println("DB connection is not valid. Exiting the process")
    exitProcess(1)
}


val ps = connectionAuth.prepareStatement("""
    insert into market_raw (label, date, open, high, low, close, volume) VALUES(?, ?, ?, ?, ?, ?, ?)
""".trimIndent())

val sqlQueryRater =  connectionAuth.prepareStatement(LATEST_AVAILIBLE_SQL)
val result = sqlQueryRater.executeQuery()
while(result.next()){

    val id = result.getString("id")
    val date = result.getString("date")
    println(">>> id=$id, date=$date")
}


/*val timestamp = Timestamp(Calendar.getInstance().timeInMillis)
val sqlQueryRater =  connectionRater.prepareStatement("select id, path, customer_account, is_deleted from subscriber")
val result = sqlQueryRater.executeQuery()
while(result.next()){

    val id = result.getString("id")
    val rawPath = result.getString("path")
    val customerAccount = result.getString("customer_account")?.let{UUID.fromString(it)}
    val path = rawPath?.let {
        if (it.isBlank())
            "$id"
        else "$it/$id"
    } ?: "$id"
    val isDeleted = result.getBoolean("is_deleted")


    ps.setObject(1,UUID.fromString(id))
    ps.setTimestamp(2,timestamp)
    ps.setTimestamp(3,timestamp)
    ps.setString(4,path)
    ps.setObject(5,customerAccount)
    ps.setBoolean(6,isDeleted)
    ps.addBatch()

}
println("--- finished to prepare data ${LocalDateTime.now()}")
ps.executeLargeBatch()
connectionAuth.commit()
println("--- commited")
connectionAuth.close()
connectionRater.close()*/
connectionAuth.close()
println("--- end end ${LocalDateTime.now()}")