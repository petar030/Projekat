import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ManagerService } from '../services/manager/manager-service';

import { ManagerCalendar } from './manager-calendar';

describe('ManagerCalendar', () => {
  let component: ManagerCalendar;
  let fixture: ComponentFixture<ManagerCalendar>;

  beforeEach(async () => {
    const managerServiceMock = {
      spaces: () => of({ spaces: [] }),
      calendar: () => of({ events: [] })
    };

    await TestBed.configureTestingModule({
      imports: [ManagerCalendar],
      providers: [
        { provide: ManagerService, useValue: managerServiceMock }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManagerCalendar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
