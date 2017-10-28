package tanvd.audit.implementation.clickhouse

import tanvd.aorm.*
import tanvd.aorm.query.*
import tanvd.audit.implementation.clickhouse.aorm.AuditTable
import tanvd.audit.implementation.dao.AuditDao
import tanvd.audit.implementation.exceptions.BasicDbException
import tanvd.audit.model.external.types.information.InformationType
import tanvd.audit.model.external.types.objects.ObjectType
import tanvd.audit.model.internal.AuditRecordInternal
import tanvd.audit.utils.PropertyLoader
import javax.sql.DataSource

/**
 * Dao to Clickhouse DB.
 */
internal class AuditDaoClickhouseImpl(dataSource: DataSource) : AuditDao {


    /**
     * Predefined scheme for clickhouse base.
     */
    private val useDefaultDDL by lazy { PropertyLoader["UseDefaultDDL"]?.toBoolean() ?: true }

    init {
        initTables()
    }

    /**
     * Creates necessary tables to start
     *
     * @throws BasicDbException
     */
    private fun initTables() {
        if (useDefaultDDL) {
            AuditTable.create()
        }
    }

    /**
     * Saves audit record and all its objects
     *
     * @throws BasicDbException
     */
    override fun saveRecord(auditRecordInternal: AuditRecordInternal) {
        AuditTable insert ClickhouseRecordSerializer.serialize(auditRecordInternal)
    }

    /**
     * Saves audit records. Makes it faster, than FOR loop with saveRecord
     *
     * @throws BasicDbException
     */
    override fun saveRecords(auditRecordInternals: List<AuditRecordInternal>) {
        AuditTable insertWithColumns AuditTable.columns.toList() values auditRecordInternals.map { ClickhouseRecordSerializer.serialize(it) }
    }

    /**
     * Adds new type and creates column for it
     *
     * @throws BasicDbException
     */
    override fun <T : Any> addTypeInDbModel(type: ObjectType<T>) {
        for (stateType in type.state) {
            AuditTable.addColumn(stateType.column as Column<List<T>, DbType<List<T>>>, useDefaultDDL)
        }
    }

    override fun <T : Any> addInformationInDbModel(information: InformationType<T>) {
        AuditTable.addColumn(information.column, useDefaultDDL)
    }

    /**
     * Loads all auditRecords with specified object except Deleted
     *
     * @throws BasicDbException
     */
    override fun loadRecords(expression: QueryExpression, limitExpression: LimitExpression,
                             orderByExpression: OrderByExpression): List<AuditRecordInternal> {
        val query = AuditTable.select() prewhere expression limit limitExpression orderBy orderByExpression
        val resultList = query.toResult()
        return resultList.map { ClickhouseRecordSerializer.deserialize(it) }
    }

    /**
     * Return total count of records satisfying condition except Deleted
     *
     * @throws BasicDbException
     */
    override fun countRecords(expression: QueryExpression): Long {
        val alias = "cnt"
        val resultList = AuditTable.select(Count(AuditTable.date, alias)).toResult()
        return resultList.single()[alias] as Long
    }

    override fun resetTable() {
        AuditTable.drop()
        AuditTable.create()
    }

//    override fun finalize() {
//        clickhouseConnection.close()
//    }


    /**
     * Drops table with specified name
     *
     * @throws BasicDbException
     */
    fun dropTable(tableName: String) {
        AuditTable.drop()
    }
}

