package ee.oyatl.ime.make.module.inputengine

import ee.oyatl.ime.make.preset.table.CharOverrideTable
import ee.oyatl.ime.make.preset.table.CodeConvertTable
import ee.oyatl.ime.make.preset.table.MoreKeysTable

interface TableInputEngine: InputEngine {
    val convertTable: CodeConvertTable
    val overrideTable: CharOverrideTable
    val moreKeysTable: MoreKeysTable
}