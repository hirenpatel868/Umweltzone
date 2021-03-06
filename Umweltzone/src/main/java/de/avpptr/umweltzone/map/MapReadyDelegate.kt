/*
 *  Copyright (C) 2019  Tobias Preuss
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.avpptr.umweltzone.map

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import de.avpptr.umweltzone.models.AdministrativeZone
import de.avpptr.umweltzone.models.extensions.isValid
import de.avpptr.umweltzone.prefs.PreferencesHelper

internal class MapReadyDelegate(

        private val preferencesHelper: PreferencesHelper,
        private val getCenterZoneRequested: () -> Boolean,
        private val setCenterZoneRequested: (centerZoneRequested: Boolean) -> Unit,
        private val getDefaultAdministrativeZone: () -> AdministrativeZone,
        private val listener: Listener

) {

    fun evaluate() {
        if (preferencesHelper.storesZoneIsDrawable()) {
            if (preferencesHelper.restoreZoneIsDrawable()) {
                listener.onDrawPolygonOverlay()
            } else {
                listener.onShowZoneNotDrawableDialog()
            }
        }
        if (getCenterZoneRequested.invoke()) {
            // City has been selected from the list
            val lastKnownPosition = preferencesHelper.restoreLastKnownLocationAsBoundingBox()
            if (lastKnownPosition.isValid) {
                listener.onZoomToBounds(lastKnownPosition.toLatLngBounds())
            }
            setCenterZoneRequested.invoke(false)
        } else {
            val lastKnownCameraPosition = preferencesHelper.restoreCameraPosition()
            if (lastKnownCameraPosition.isValid()) {
                listener.onZoomToLocation(lastKnownCameraPosition)
            } else {
                // Select default city at first application start
                getDefaultAdministrativeZone.invoke().let {
                    storeLastAdministrativeZone(it)
                    if (preferencesHelper.storesZoneIsDrawable() && preferencesHelper.restoreZoneIsDrawable()) {
                        listener.onDrawPolygonOverlay()
                    } else {
                        listener.onShowZoneNotDrawableDialog()
                    }
                    listener.onZoomToBounds(it.boundingBox.toLatLngBounds())
                    listener.onStoreLastMapState()
                }
            }
        }
    }

    private fun storeLastAdministrativeZone(defaultAdministrativeZone: AdministrativeZone) =
            preferencesHelper.storeAdministrativeZone(defaultAdministrativeZone)

    internal interface Listener {

        fun onDrawPolygonOverlay()

        fun onShowZoneNotDrawableDialog()

        fun onStoreLastMapState()

        fun onZoomToBounds(latLngBounds: LatLngBounds)

        fun onZoomToLocation(cameraPosition: CameraPosition)

    }

}
