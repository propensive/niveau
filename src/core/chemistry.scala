package charisma

import gossamer.*
import rudiments.*
import fulminate.*
import anticipation.*
import perforate.*
import hieroglyph.*
import spectacular.*

import scala.quoted.*

object PeriodicTable:
  val H = ChemicalElement(1,   t"H",  t"Hydrogen")
  val He = ChemicalElement(2,   t"He", t"Helium")
  val Li = ChemicalElement(3,   t"Li", t"Lithium")
  val Be = ChemicalElement(4,   t"Be", t"Beryllium")
  val B = ChemicalElement(5,   t"B",  t"Boron")
  val C = ChemicalElement(6,   t"C",  t"Carbon")
  val N = ChemicalElement(7,   t"N",  t"Nitrogen")
  val O = ChemicalElement(8,   t"O",  t"Oxygen")
  val F = ChemicalElement(9,   t"F",  t"Fluorine")
  val Ne = ChemicalElement(10,  t"Ne", t"Neon")
  val Na = ChemicalElement(11,  t"Na", t"Sodium")
  val Mg = ChemicalElement(12,  t"Mg", t"Magnesium")
  val Al = ChemicalElement(13,  t"Al", t"Aluminium")
  val Si = ChemicalElement(14,  t"Si", t"Silicon")
  val P = ChemicalElement(15,  t"P",  t"Phosphorus")
  val S = ChemicalElement(16,  t"S",  t"Sulphur")
  val Cl = ChemicalElement(17,  t"Cl", t"Chlorine")
  val Ar = ChemicalElement(18,  t"Ar", t"Argon")
  val K = ChemicalElement(19,  t"K",  t"Potassium")
  val Ca = ChemicalElement(20,  t"Ca", t"Calcium")
  val Sc = ChemicalElement(21,  t"Sc", t"Scandium")
  val Ti = ChemicalElement(22,  t"Ti", t"Titanium")
  val V = ChemicalElement(23,  t"V",  t"Vanadium")
  val Cr = ChemicalElement(24,  t"Cr", t"Chromium")
  val Mn = ChemicalElement(25,  t"Mn", t"Manganese")
  val Fe = ChemicalElement(26,  t"Fe", t"Iron")
  val Co = ChemicalElement(27,  t"Co", t"Cobalt")
  val Ni = ChemicalElement(28,  t"Ni", t"Nickel")
  val Cu = ChemicalElement(29,  t"Cu", t"Copper")
  val Zn = ChemicalElement(30,  t"Zn", t"Zinc")
  val Ga = ChemicalElement(31,  t"Ga", t"Gallium")
  val Ge = ChemicalElement(32,  t"Ge", t"Germanium")
  val As = ChemicalElement(33,  t"As", t"Arsenic")
  val Se = ChemicalElement(34,  t"Se", t"Selenium")
  val Br = ChemicalElement(35,  t"Br", t"Bromine")
  val Kr = ChemicalElement(36,  t"Kr", t"Krypton")
  val Rb = ChemicalElement(37,  t"Rb", t"Rubidium")
  val Sr = ChemicalElement(38,  t"Sr", t"Strontium")
  val Y = ChemicalElement(39,  t"Y",  t"Yttrium")
  val Zr = ChemicalElement(40,  t"Zr", t"Zirconium")
  val Nb = ChemicalElement(41,  t"Nb", t"Niobium")
  val Mo = ChemicalElement(42,  t"Mo", t"Molybdenum")
  val Tc = ChemicalElement(43,  t"Tc", t"Technetium")
  val Ru = ChemicalElement(44,  t"Ru", t"Ruthenium")
  val Rh = ChemicalElement(45,  t"Rh", t"Rhodium")
  val Pd = ChemicalElement(46,  t"Pd", t"Palladium")
  val Ag = ChemicalElement(47,  t"Ag", t"Silver")
  val Cd = ChemicalElement(48,  t"Cd", t"Cadmium")
  val In = ChemicalElement(49,  t"In", t"Indium")
  val Sn = ChemicalElement(50,  t"Sn", t"Tin")
  val Sb = ChemicalElement(51,  t"Sb", t"Antimony")
  val Te = ChemicalElement(52,  t"Te", t"Tellurium")
  val I = ChemicalElement(53,  t"I",  t"Iodine")
  val Xe = ChemicalElement(54,  t"Xe", t"Xenon")
  val Cs = ChemicalElement(55,  t"Cs", t"Cesium")
  val Ba = ChemicalElement(56,  t"Ba", t"Barium")
  val La = ChemicalElement(57,  t"La", t"Lanthanum")
  val Ce = ChemicalElement(58,  t"Ce", t"Cerium")
  val Pr = ChemicalElement(59,  t"Pr", t"Praseodymium")
  val Nd = ChemicalElement(60,  t"Nd", t"Neodymium")
  val Pm = ChemicalElement(61,  t"Pm", t"Promethium")
  val Sm = ChemicalElement(62,  t"Sm", t"Samarium")
  val Eu = ChemicalElement(63,  t"Eu", t"Europium")
  val Gd = ChemicalElement(64,  t"Gd", t"Gadolinium")
  val Tb = ChemicalElement(65,  t"Tb", t"Terbium")
  val Dy = ChemicalElement(66,  t"Dy", t"Dysprosium")
  val Ho = ChemicalElement(67,  t"Ho", t"Holmium")
  val Er = ChemicalElement(68,  t"Er", t"Erbium")
  val Tm = ChemicalElement(69,  t"Tm", t"Thulium")
  val Yb = ChemicalElement(70,  t"Yb", t"Ytterbium")
  val Lu = ChemicalElement(71,  t"Lu", t"Lutetium")
  val Hf = ChemicalElement(72,  t"Hf", t"Hafnium")
  val Ta = ChemicalElement(73,  t"Ta", t"Tantalum")
  val W = ChemicalElement(74,  t"W",  t"Tungsten")
  val Re = ChemicalElement(75,  t"Re", t"Rhenium")
  val Os = ChemicalElement(76,  t"Os", t"Osmium")
  val Ir = ChemicalElement(77,  t"Ir", t"Iridium")
  val Pt = ChemicalElement(78,  t"Pt", t"Platinum")
  val Au = ChemicalElement(79,  t"Au", t"Gold")
  val Hg = ChemicalElement(80,  t"Hg", t"Mercury")
  val Tl = ChemicalElement(81,  t"Tl", t"Thallium")
  val Pb = ChemicalElement(82,  t"Pb", t"Lead")
  val Bi = ChemicalElement(83,  t"Bi", t"Bismuth")
  val Po = ChemicalElement(84,  t"Po", t"Polonium")
  val At = ChemicalElement(85,  t"At", t"Astatine")
  val Rn = ChemicalElement(86,  t"Rn", t"Radon")
  val Fr = ChemicalElement(87,  t"Fr", t"Francium")
  val Ra = ChemicalElement(88,  t"Ra", t"Radium")
  val Ac = ChemicalElement(89,  t"Ac", t"Actinium")
  val Th = ChemicalElement(90,  t"Th", t"Thorium")
  val Pa = ChemicalElement(91,  t"Pa", t"Protactinium")
  val U = ChemicalElement(92,  t"U",  t"Uranium")
  val Np = ChemicalElement(93,  t"Np", t"Neptunium")
  val Pu = ChemicalElement(94,  t"Pu", t"Plutonium")
  val Am = ChemicalElement(95,  t"Am", t"Americium")
  val Cm = ChemicalElement(96,  t"Cm", t"Curium")
  val Bk = ChemicalElement(97,  t"Bk", t"Berkelium")
  val Cf = ChemicalElement(98,  t"Cf", t"Californium")
  val Es = ChemicalElement(99,  t"Es", t"Einsteinium")
  val Fm = ChemicalElement(100, t"Fm", t"Fermium")
  val Md = ChemicalElement(101, t"Md", t"Mendelevium")
  val No = ChemicalElement(102, t"No", t"Nobelium")
  val Lr = ChemicalElement(103, t"Lr", t"Lawrencium")
  val Rf = ChemicalElement(104, t"Rf", t"Rutherfordium")
  val Db = ChemicalElement(105, t"Db", t"Dubnium")
  val Sg = ChemicalElement(106, t"Sg", t"Seaborgium")
  val Bh = ChemicalElement(107, t"Bh", t"Bohrium")
  val Hs = ChemicalElement(108, t"Hs", t"Hassium")
  val Mt = ChemicalElement(109, t"Mt", t"Meitnerium")
  val Ds = ChemicalElement(110, t"Ds", t"Darmstadtium")
  val Rg = ChemicalElement(111, t"Rg", t"Roentgenium")
  val Cn = ChemicalElement(112, t"Cn", t"Copernicium")
  val Nh = ChemicalElement(113, t"Nh", t"Nihonium")
  val Fl = ChemicalElement(114, t"Fl", t"Flerovium")
  val Mc = ChemicalElement(115, t"Mc", t"Moscovium")
  val Lv = ChemicalElement(116, t"Lv", t"Livermorium")
  val Ts = ChemicalElement(117, t"Ts", t"Tennessine")
  val Og = ChemicalElement(118, t"Og", t"Oganesson")

  val elements: IArray[ChemicalElement] = IArray(
    H, He, Li, Be, B, C, N, O, F, Ne, Na, Mg, Al, Si, P, S, Cl, Ar, K, Ca, Sc, Ti, V, Cr, Mn, Fe, Co, Ni, Cu,
    Zn, Ga, Ge, As, Se, Br, Kr, Rb, Sr, Y, Zr, Nb, Mo, Tc, Ru, Rh, Pd, Ag, Cd, In, Sn, Sb, Te, I, Xe, Cs, Ba,
    La, Ce, Pr, Nd, Pm, Sm, Eu, Gd, Tb, Dy, Ho, Er, Tm, Yb, Lu, Hf, Ta, W, Re, Os, Ir, Pt, Au, Hg, Tl, Pb, Bi,
    Po, At, Rn, Fr, Ra, Ac, Th, Pa, U, Np, Pu, Am, Cm, Bk, Cf, Es, Fm, Md, No, Lr, Rf, Db, Sg, Bh, Hs, Mt, Ds,
    Rg, Cn, Nh, Fl, Mc, Lv, Ts, Og
  )

  lazy val symbols: Map[Text, ChemicalElement] = unsafely(elements.indexBy(_.symbol))

  def apply(number: Int): Maybe[ChemicalElement] = if 1 <= number <= 118 then elements(number - 1) else Unset
  def apply(symbol: Text): Maybe[ChemicalElement] = symbols.getOrElse(symbol, Unset)

case class ChemicalElement(number: Int, symbol: Text, name: Text):
  @targetName("sub")
  def *(count: Int): Molecule = Molecule(1, Map(this -> count))

object Molecule:
  given Show[Molecule] = molecule =>
    val orderedElements =
      if !molecule.elements.contains(PeriodicTable.C)
      then molecule.elements.to(List).sortBy(_(0).symbol)
      else
        val carbon = PeriodicTable.C -> molecule.elements(PeriodicTable.C)
        
        val hydrogen =
          if !molecule.elements.contains(PeriodicTable.H) then Nil else
            List(PeriodicTable.H -> molecule.elements(PeriodicTable.H))
        
        val rest =
          (molecule.elements - PeriodicTable.C - PeriodicTable.H).to(List).sortBy(_(0).symbol)
        
        carbon :: hydrogen ::: rest
    
    orderedElements.map: (element, count) =>
      val number = if count == 1 then t"" else count.show.chars.map(_.subscript).sift[Char].map(_.show).join
      t"${element.symbol}$number"
    .join(if molecule.count == 1 then t"" else molecule.count.show, t"", t"")

case class Molecule(count: Int, elements: Map[ChemicalElement, Int]):
  @targetName("and")
  def &(other: Molecule): Molecule =
    val elements2 = other.elements.foldLeft(elements): (acc, next) =>
      acc.updated(next(0), elements.getOrElse(next(0), 0) + next(1))
    
    Molecule(count, elements2)

  @targetName("and2")
  def &(element: ChemicalElement): Molecule =
    this & Molecule(1, Map(element -> 1))
  
  @targetName("times")
  def *(count2: Int): Molecule = Molecule(count*count2, elements)

object Charisma:
  def element(context: Expr[StringContext])(using Quotes): Expr[ChemicalElement] =
    import quotes.reflect.*
    val symbol = context.valueOrAbort.parts.head.tt
    
    if !PeriodicTable.symbols.contains(symbol)
    then fail(msg"there is no known chemical element with the symbol ${symbol}")
    else '{PeriodicTable(${Expr(symbol)}).avow(using Unsafe)}

extension (inline context: StringContext)
  inline def e(): ChemicalElement = ${Charisma.element('context)}
