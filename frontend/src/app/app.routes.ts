import { Routes } from '@angular/router';
import { LoginComponent } from './login-component/login-component';
import { AdminLoginComponent } from './admin-login-component/admin-login-component';
import { RecoveryComponent } from './recovery-component/recovery-component';
import { ForgottenPasswordComponent } from './forgotten-password-component/forgotten-password-component';
import { RegistrationComponent } from './registration-component/registration-component';
import { StartComponent } from './start-component/start-component';
import { PublicDetailsComponent } from './public-details-component/public-details-component';
import { MemberComponent } from './member-component/member-component';
import { MemberDetailsComponent } from './member-details-component/member-details-component';
import { ManagerAddComponent } from './manager-add-component/manager-add-component';
import { ManagerComponent } from './manager-component/manager-component';
import { ManagerUpdateComponent } from './manager-update-component/manager-update-component';
import { ManagerCalendar } from './manager-calendar/manager-calendar';
import { AdminComponent } from './admin-component/admin-component';

export const routes: Routes = [
    {path: '', component: StartComponent},
    {path: 'public_details/:id', component: PublicDetailsComponent},
    {path: 'login', component: LoginComponent},
    {path: 'admin_login', component: AdminLoginComponent},
    {path: 'register', component: RegistrationComponent},
    {path: 'forgot-password', component: ForgottenPasswordComponent},
    {path: 'recovery/:token', component: RecoveryComponent},
    {path: 'member', component: MemberComponent},
    {path: 'member_details/:id', component: MemberDetailsComponent},
    {path: 'manager', component: ManagerComponent},
    {path: 'manager_calendar', component: ManagerCalendar},
    {path: 'manager_add', component: ManagerAddComponent},
    {path: 'manager_update/:id', component: ManagerUpdateComponent},
    {path: 'admin', component: AdminComponent}
];
