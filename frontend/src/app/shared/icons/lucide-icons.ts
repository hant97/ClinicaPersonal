/**
 * Punto único de origen para los íconos de `lucide-angular` usados en la app.
 *
 * Antes, cada componente standalone repetía su propia lista de 15-25+ íconos
 * importados directamente desde 'lucide-angular', duplicando el mismo bloque
 * de imports en decenas de archivos. Este barrel reexporta todos los íconos
 * que la app utiliza, para que cada componente los importe desde un solo
 * módulo interno (`shared/icons/lucide-icons`) en vez de enumerarlos sueltos
 * desde el paquete externo.
 *
 * `LucideAngularModule` NO se reexporta aquí a propósito: es el módulo
 * standalone que declara el componente `<lucide-icon>`, y el compilador de
 * Angular emite, para el chequeo de tipos de plantillas, una ruta relativa
 * calculada contra el archivo que la importa. Reexportarlo a través de este
 * barrel hace que esa ruta se calcule mal en componentes ubicados a distinta
 * profundidad de carpetas, rompiendo el build (`Could not resolve
 * ".../node_modules/lucide-angular/lib/lucide-angular.component"`). Por eso
 * cada componente sigue importando `LucideAngularModule` directo desde
 * 'lucide-angular' — solo los íconos (clases planas, sin este problema) se
 * centralizan aquí.
 *
 * No cambia el patrón de uso en las plantillas: cada componente sigue
 * declarando sus propios campos `readonly NombreIcono = NombreIcono;` para
 * los íconos que realmente usa (las plantillas los referencian por nombre de
 * propiedad).
 *
 * Para agregar un ícono nuevo: reexportarlo aquí una sola vez y luego
 * importarlo desde este archivo en el componente que lo necesite.
 */
export {
  Activity,
  AlertCircle,
  AlertTriangle,
  ArrowDown,
  ArrowLeft,
  ArrowLeftRight,
  ArrowRight,
  ArrowUp,
  Banknote,
  BarChart3,
  BellRing,
  BookOpen,
  Brain,
  Briefcase,
  Building,
  Building2,
  Calendar,
  CalendarCheck,
  CalendarClock,
  CalendarDays,
  CalendarOff,
  CalendarPlus,
  CalendarRange,
  Check,
  CheckCheck,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  Circle,
  ClipboardList,
  Clock,
  Coins,
  Copy,
  CreditCard,
  DollarSign,
  Download,
  Edit,
  Edit2,
  ExternalLink,
  Eye,
  EyeOff,
  FileText,
  Filter,
  FilterX,
  FolderOpen,
  FolderTree,
  Globe,
  Heart,
  HeartHandshake,
  HeartPulse,
  History,
  Image,
  ImagePlus,
  Info,
  Key,
  KeyRound,
  Landmark,
  Layers,
  LayoutDashboard,
  LayoutGrid,
  LayoutList,
  List,
  Lock,
  LogOut,
  Mail,
  MapPin,
  Menu,
  MessageCircle,
  Microscope,
  Minus,
  Monitor,
  MoreHorizontal,
  MoreVertical,
  Package,
  Phone,
  Pill,
  Play,
  Plus,
  PlusCircle,
  Printer,
  QrCode,
  Receipt,
  RefreshCw,
  Repeat,
  RotateCcw,
  RotateCw,
  Save,
  Search,
  Settings,
  Settings2,
  Shield,
  ShieldAlert,
  ShieldCheck,
  SlidersHorizontal,
  Smartphone,
  Sparkles,
  Stethoscope,
  Tablet,
  Tag,
  Trash2,
  TrendingDown,
  TrendingUp,
  Upload,
  User,
  UserCheck,
  UserCog,
  UserPlus,
  UserRound,
  UserX,
  Users,
  Video,
  Wallet,
  X,
  XCircle,
  Zap,
} from 'lucide-angular';
