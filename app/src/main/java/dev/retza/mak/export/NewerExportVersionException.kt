package dev.retza.mak.export

/** The file comes from a newer MAK; its other fields may be unknown to this version, so they are not read. */
class NewerExportVersionException(version: Int) :
    IllegalArgumentException("Export schema version $version is newer than ${ExportSchema.VERSION}.")
