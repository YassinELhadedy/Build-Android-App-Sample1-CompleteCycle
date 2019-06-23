package com.transporter.streetglide.infrastructure

import com.transporter.streetglide.models.Sheet

/**
 * SheetProxyRepository
 */
class SheetProxyRepository(sheetDiskRepository: SheetDiskRepository,
                           sheetSuperGlideRepository: SheetSuperGlideRepository) :
        AbstractProxyGetRepository<Sheet, Sheet>(sheetDiskRepository,
                sheetSuperGlideRepository) {

    override fun convert(entity: Sheet): Sheet = entity
}