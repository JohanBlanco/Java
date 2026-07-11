export interface AuthResponse {
  token: string
  userId: number
  email: string
  firstName: string
  lastName: string
  roles: string[]
  organizationId: number | null
}

export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  roles: string[]
  organizationId: number | null
  active: boolean
  profile?: MemberProfile
}

export interface MemberProfile {
  birthYear?: number
  age?: number
  goals?: string
  phone?: string
  emergencyContact?: string
}

export interface Organization {
  id: number
  name: string
  slug: string
  contactEmail: string
  contactPhone: string
  subscriptionStatus: string
  active: boolean
  ownerFirstName?: string
  ownerLastName?: string
  ownerEmail?: string
}

export type GymStaffRole = 'GYM_OWNER' | 'RECEPTIONIST' | 'INSTRUCTOR' | 'MEMBER'

export interface PackageAddon {
  id: number
  name: string
  description: string
  price: number
  active: boolean
}

export interface MembershipPackage {
  id: number
  name: string
  description: string
  price: number
  durationMonths: number
  freeActivityQuota: number | null
  active: boolean
  addons: PackageAddon[]
}

export interface Activity {
  id: number
  name: string
  description: string
  activityDate: string
  startDate: string
  endDate: string
  recurring: boolean
  repeatDays: string[]
  startTime: string
  endTime: string
  locationName: string
  instructorId: number | null
  instructorName: string | null
  capacity: number | null
  confirmedReservations: number
  pendingReservations: number
  hasCapacity: boolean
  hasOccurrenceOverride: boolean
  active: boolean
}

export interface MembershipUsage {
  membershipPackageId: number | null
  membershipName: string | null
  freeActivityQuota: number | null
  freeActivitiesUsed: number
  freeActivitiesRemaining: number | null
  unlimitedFreeActivities: boolean
}

export interface Reservation {
  id: number
  activityId: number
  activityName: string
  occurrenceDate: string
  memberId: number
  memberName: string
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  freeSlot: boolean
  paymentRequired: boolean
  paid: boolean
  attended: boolean
  createdAt: string
}

export interface ActivityReservationImpact {
  activeReservations: number
  affectedReservations: number
  items: {
    reservationId: number
    occurrenceDate: string
    memberName: string
    status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  }[]
}

export interface Sale {
  id: number
  memberName: string
  activityName: string
  concept: string
  amount: number
  paidAt: string
}

export interface GymStats {
  memberCount: number
  activitiesScheduled: number
  activitiesToday: number
  reservationsToday: number
  confirmedReservations: number
  pendingPayments: number
  salesToday: number
  salesThisMonth: number
  attendancesThisMonth: number
}

export interface RoutineExercise {
  id: number
  exerciseName: string
  sets: number
  reps: number
  weight?: number
  durationSeconds?: number
  notes?: string
  orderIndex: number
}

export interface Routine {
  id: number
  name: string
  description: string
  notes?: string
  memberId: number
  memberName: string
  instructorId: number
  instructorName: string
  templateId: number | null
  temporary: boolean
  exercises: RoutineExercise[]
}

export interface RoutineRequest {
  id: number
  memberId: number
  memberName: string
  description: string
  goals: string
  status: string
  assignedInstructorId: number | null
  assignedInstructorName: string | null
}
