import { Routes } from '@angular/router';
import { LoginComponent } from './login-component/login-component';
import { AdminLoginComponent } from './admin-login-component/admin-login-component';
import { RecoveryComponent } from './recovery-component/recovery-component';
import { ForgottenPasswordComponent } from './forgotten-password-component/forgotten-password-component';
import { RegistrationComponent } from './registration-component/registration-component';
import { StartComponent } from './start-component/start-component';

export const routes: Routes = [
    {path: '', component: StartComponent},
    {path: 'login', component: LoginComponent},
    {path: 'admin', component: AdminLoginComponent},
    {path: 'register', component: RegistrationComponent},
    {path: 'forgot-password', component: ForgottenPasswordComponent},
    {path: 'recovery/:token', component: RecoveryComponent}
];
