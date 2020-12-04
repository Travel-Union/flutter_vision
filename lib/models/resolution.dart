enum Resolution { potato, vga, hd, fullhd, ultrahd }

extension ResolutionSerializer on Resolution {
  String serialize() {
    switch (this) {
      case Resolution.ultrahd:
        return "ultrahd";
      case Resolution.fullhd:
        return "fullhd";
      case Resolution.hd:
        return "hd";
      case Resolution.vga:
        return "vga";
      case Resolution.potato:
        return "potato";
      default:
        throw Exception("Could not serialize Resolution value");
    }
  }
}