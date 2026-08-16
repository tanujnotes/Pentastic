# dmgbuild settings for the Pentastic installer DMG.
# Paths are relative to the repo root; invoked that way by make-dmg.sh.
# See https://dmgbuild.readthedocs.io

volume_name = "Pentastic"
format = "UDZO"

files = ["composeApp/build/compose/binaries/main/app/Pentastic.app"]
symlinks = {"Applications": "/Applications"}

icon = "composeApp/icons/Pentastic.icns"
background = "composeApp/dmg/dmg-background.tiff"

window_rect = ((200, 140), (600, 400))
default_view = "icon-view"
show_icon_preview = False
icon_size = 100
text_size = 13
icon_locations = {
    "Pentastic.app": (150, 200),
    "Applications": (450, 200),
}
