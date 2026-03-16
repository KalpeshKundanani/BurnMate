package org.kalpeshbkundanani.burnmate.weight.domain

import org.kalpeshbkundanani.burnmate.weight.model.WeightValue

interface WeightEntryValidator {
    fun validate(weight: WeightValue): Result<Unit>
}
