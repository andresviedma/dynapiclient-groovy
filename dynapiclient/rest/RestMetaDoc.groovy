package dynapiclient.rest

interface RestMetaDoc {
    String getResourceManual(String path)
    // For autocomplete
    Set /*<String>*/ getNextResourcePieces(String path)
}
