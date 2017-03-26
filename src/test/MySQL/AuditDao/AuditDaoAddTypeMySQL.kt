package MySQL.AuditDao

import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import tanvd.audit.implementation.dao.DbType
import tanvd.audit.implementation.mysql.AuditDaoMysqlImpl
import tanvd.audit.model.external.AuditType
import tanvd.audit.model.external.AuditType.AuditSerializer
import tanvd.audit.model.external.AuditType.TypesResolution.addType
import tanvd.audit.model.external.AuditType.TypesResolution.resolveType
import tanvd.audit.model.external.QueryParameters
import tanvd.audit.model.external.equal
import tanvd.audit.model.internal.AuditRecord
import tanvd.audit.serializers.IntSerializer
import tanvd.audit.serializers.StringSerializer


internal class AuditDaoAddTypeMySQL() {

    companion object {
        var auditDao: AuditDaoMysqlImpl? = null
    }

    class TestClassFirst {
        companion object serializer : AuditSerializer<TestClassFirst> {
            override fun deserialize(serializedString: String): TestClassFirst {
                throw UnsupportedOperationException("not implemented")
            }

            override fun serialize(value: TestClassFirst): String {
                return "TestClassFirstId"
            }

        }
    }

    @BeforeMethod
    @Suppress("UNCHECKED_CAST")
    fun createAll() {
        auditDao = DbType.MySQL.getDao("jdbc:mysql://localhost/example?useLegacyDatetimeCode=false" +
                "&serverTimezone=Europe/Moscow", "root", "root") as AuditDaoMysqlImpl

        val typeString = AuditType(String::class, "Type_String", StringSerializer) as AuditType<Any>
        addType(typeString)
        auditDao!!.addTypeInDbModel(typeString)

        val typeInt = AuditType(Int::class, "Type_Int", IntSerializer) as AuditType<Any>
        addType(typeInt)
        auditDao!!.addTypeInDbModel(typeInt)
    }

    @AfterMethod
    fun clearAll() {
        auditDao!!.dropTable(AuditDaoMysqlImpl.auditTable)
        for (type in AuditType.getTypes()) {
            auditDao!!.dropTable(type.code)
        }
        AuditType.clearTypes()
    }

    @Test
    fun saveRowBefore_addType_loadedNormally() {
        val arrayObjects = arrayListOf(Pair(resolveType(String::class), "string"))
        val auditRecordOriginal = AuditRecord(arrayObjects, 127)
        auditDao!!.saveRecord(auditRecordOriginal)

        @Suppress("UNCHECKED_CAST")
        val typeTestClassFirst = AuditType(TestClassFirst::class, "TestClassFirst", TestClassFirst) as AuditType<Any>
        AuditType.addType(typeTestClassFirst)
        auditDao!!.addTypeInDbModel(typeTestClassFirst)

        val recordsLoaded = auditDao!!.loadRecords(String::class equal "string", QueryParameters())
        Assert.assertEquals(recordsLoaded, listOf(auditRecordOriginal))
    }

    @Test
    fun saveRowBefore_addTypeAndSaveWithNewType_loadedNormallyNotNew() {
        val arrayObjectsFirst = arrayListOf(Pair(resolveType(String::class), "string"))
        val auditRecordFirstOriginal = AuditRecord(arrayObjectsFirst, 127)
        auditDao!!.saveRecord(auditRecordFirstOriginal)

        @Suppress("UNCHECKED_CAST")
        val typeTestClassFirst = AuditType(TestClassFirst::class, "TestClassFirst", TestClassFirst) as AuditType<Any>
        AuditType.addType(typeTestClassFirst)
        auditDao!!.addTypeInDbModel(typeTestClassFirst)

        val arrayObjectsSecond = arrayListOf(Pair(resolveType(TestClassFirst::class), "TestClassFirstId"))
        val auditRecordSecondOriginal = AuditRecord(arrayObjectsSecond, 127)
        auditDao!!.saveRecord(auditRecordSecondOriginal)

        val recordsLoaded = auditDao!!.loadRecords(String::class equal "string", QueryParameters())

        Assert.assertEquals(recordsLoaded, listOf(auditRecordFirstOriginal))
    }

    @Test
    fun saveRowBefore_addTypeAndSaveWithNewType_loadedNormallyNew() {
        val arrayObjectsFirst = arrayListOf(Pair(resolveType(String::class), "string"))
        val auditRecordFirstOriginal = AuditRecord(arrayObjectsFirst, 127)
        auditDao!!.saveRecord(auditRecordFirstOriginal)

        @Suppress("UNCHECKED_CAST")
        val typeTestClassFirst = AuditType(TestClassFirst::class, "TestClassFirst", TestClassFirst) as AuditType<Any>
        AuditType.addType(typeTestClassFirst)
        auditDao!!.addTypeInDbModel(typeTestClassFirst)

        val arrayObjectsSecond = arrayListOf(Pair(resolveType(TestClassFirst::class), "TestClassFirstId"))
        val auditRecordSecondOriginal = AuditRecord(arrayObjectsSecond, 127)
        auditDao!!.saveRecord(auditRecordSecondOriginal)


        val recordsLoaded = auditDao!!.loadRecords(TestClassFirst::class equal TestClassFirst(), QueryParameters())
        Assert.assertEquals(recordsLoaded, listOf(auditRecordSecondOriginal))
    }

    @Test
    fun saveRowBefore_addTypeAndSaveWithNewType_loadedNormallyBoth() {
        val arrayObjectsFirst = arrayListOf(Pair(resolveType(String::class), "string"))
        val auditRecordFirstOriginal = AuditRecord(arrayObjectsFirst, 127)
        auditDao!!.saveRecord(auditRecordFirstOriginal)

        @Suppress("UNCHECKED_CAST")
        val typeTestClassFirst = AuditType(TestClassFirst::class, "TestClassFirst", TestClassFirst) as AuditType<Any>
        AuditType.addType(typeTestClassFirst)
        auditDao!!.addTypeInDbModel(typeTestClassFirst)

        val arrayObjectsSecond = arrayListOf(
                Pair(resolveType(String::class), "string"),
                Pair(resolveType(TestClassFirst::class), "TestClassFirstId"))
        val auditRecordSecondOriginal = AuditRecord(arrayObjectsSecond, 127)
        auditDao!!.saveRecord(auditRecordSecondOriginal)


        val recordsLoaded = auditDao!!.loadRecords(String::class equal "string", QueryParameters())
        Assert.assertEquals(recordsLoaded.toSet(), setOf(auditRecordFirstOriginal, auditRecordSecondOriginal))
    }
}